package com.soomgil.common.cqrs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CqrsContractTest {

	@Test
	void commandHandlerHandlesTypedCommand() {
		CreateThingHandler handler = new CreateThingHandler();

		CreateThingResult result = handler.handle(new CreateThingCommand("route"));

		assertThat(result.value()).isEqualTo("created:route");
	}

	@Test
	void queryHandlerHandlesTypedQuery() {
		FindThingHandler handler = new FindThingHandler();

		ThingView result = handler.handle(new FindThingQuery("trip-1"));

		assertThat(result.name()).isEqualTo("thing:trip-1");
	}

	private record CreateThingCommand(String name) implements Command<CreateThingResult> {
	}

	private record CreateThingResult(String value) {
	}

	private static class CreateThingHandler implements CommandHandler<CreateThingCommand, CreateThingResult> {

		@Override
		public CreateThingResult handle(CreateThingCommand command) {
			return new CreateThingResult("created:" + command.name());
		}
	}

	private record FindThingQuery(String id) implements Query<ThingView> {
	}

	private record ThingView(String name) {
	}

	private static class FindThingHandler implements QueryHandler<FindThingQuery, ThingView> {

		@Override
		public ThingView handle(FindThingQuery query) {
			return new ThingView("thing:" + query.id());
		}
	}
}
