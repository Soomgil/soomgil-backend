#!/usr/bin/env node
// 데모 v2 생성 전용: DB를 완전히 비우고 Flyway 스키마(+전국 참조 데이터)만 다시 올린다. 시드 dump는 적용하지 않는다.
import { spawnSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..');
const env = { ...parseEnv(join(rootDir, '.env')), ...process.env };
const dbUser = env.DB_USERNAME || 'soomgil';
const dbName = env.DB_NAME || 'soomgil';
const maxWait = Number(env.SEED_WAIT_SECONDS || 300);

function parseEnv(p) { const o = {}; if (!existsSync(p)) return o; for (const l of readFileSync(p, 'utf8').split('\n')) { const m = l.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/); if (m) o[m[1]] = m[2].replace(/^['"]|['"]$/g, ''); } return o; }
function run(cmd, args, opts = {}) { const r = spawnSync(cmd, args, { cwd: rootDir, stdio: opts.quiet ? 'pipe' : 'inherit', encoding: 'utf8' }); if (r.status !== 0 && !opts.allowFailure) { console.error(r.stdout, r.stderr); process.exit(r.status || 1); } return r; }
function pg() { return run('docker', ['compose', 'ps', '-q', 'postgres'], { quiet: true }).stdout.trim(); }
function scalar(cid, sql) { return run('docker', ['exec', '-i', cid, 'psql', '-U', dbUser, '-d', dbName, '-X', '-At', '-c', sql], { quiet: true, allowFailure: true }).stdout.trim(); }
function sleep(ms) { spawnSync('sleep', [String(ms / 1000)]); }

const cid = pg();
if (!cid) { console.error('postgres container not running (docker compose --profile full up -d postgres)'); process.exit(1); }
console.log('▶ stopping backend'); run('docker', ['compose', '--profile', 'full', 'stop', 'backend'], { allowFailure: true });
console.log(`▶ dropping/recreating database ${dbName}`);
run('docker', ['exec', cid, 'dropdb', '-U', dbUser, '--if-exists', '--force', dbName]);
run('docker', ['exec', cid, 'createdb', '-U', dbUser, '-O', dbUser, dbName]);
console.log('▶ starting backend (Flyway migrations: schema + nationwide regions/places/tags)');
run('docker', ['compose', '--profile', 'full', 'up', '-d', 'backend']);
const started = Date.now();
for (;;) {
  const ok = scalar(cid, "SELECT (to_regclass('auth.users') IS NOT NULL AND to_regclass('community.posts') IS NOT NULL AND to_regclass('tourism_source.attractions') IS NOT NULL AND (SELECT count(*) FROM tourism_source.attractions) > 50000 AND (SELECT count(*) FROM geo.legal_regions) > 20000)::int;");
  if (ok === '1') break;
  if ((Date.now() - started) / 1000 > maxWait) { console.error('timed out waiting for migrations'); process.exit(1); }
  sleep(5000);
}
// 백엔드 HTTP까지 뜨는 것을 기다린다.
for (let i = 0; i < 60; i++) { const r = spawnSync('curl', ['-s', '-o', '/dev/null', '-w', '%{http_code}', 'http://localhost:8080/actuator/health'], { encoding: 'utf8' }); if (r.stdout.trim() === '200') break; sleep(3000); }
console.log(`✔ fresh schema ready: users=${scalar(cid, 'SELECT count(*) FROM auth.users')} attractions=${scalar(cid, 'SELECT count(*) FROM tourism_source.attractions')} regions=${scalar(cid, 'SELECT count(*) FROM geo.legal_regions')}`);
