package com.whop.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whop.backend.transaction.TransactionRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTests {
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private TransactionRepository transactionRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void signupLoginMeLogoutFlowWorks() throws Exception {
		String signupBody = authBody("alice_1", "password123");
		MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("alice_1"))
				.andReturn();

		MockHttpSession session = (MockHttpSession) signupResult.getRequest().getSession(false);

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("alice_1"));

		mockMvc.perform(post("/api/auth/logout").session(session))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void duplicateUsernameRejected() throws Exception {
		String signupBody = authBody("bob_1", "password123");

		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupBody))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupBody))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));
	}

	@Test
	void loginFailuresAndValidationErrorsAreHandled() throws Exception {
		String signupBody = authBody("charlie_1", "password123");
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupBody))
				.andExpect(status().isOk());

		String badLoginBody = authBody("charlie_1", "wrongpass");
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(badLoginBody))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

		String invalidBody = authBody("NO", "123");
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void tasksCanBeBrowsedPubliclyButCreatedOnlyByAuthenticatedUsers() throws Exception {
		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(taskBody("Unauth task", "This task should not be created.", "25.00")))
				.andExpect(status().isUnauthorized());

		MockHttpSession session = signup("task_owner_1");

		mockMvc.perform(post("/api/tasks")
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(taskBody("Write copy", "Write product page copy for a launch.", "150.00")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Write copy"))
				.andExpect(jsonPath("$.budgetAmount").value(150.00))
				.andExpect(jsonPath("$.budgetCurrency").value("USD"))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.owner.username").value("task_owner_1"));

		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Write copy"));
	}

	@Test
	void taskValidationAndMissingDetailAreHandled() throws Exception {
		MockHttpSession session = signup("task_owner_2");

		mockMvc.perform(post("/api/tasks")
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(taskBody("No", "Too short", "0.00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(get("/api/tasks/" + UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
	}

	@Test
	void offersRequireAuthentication() throws Exception {
		MockHttpSession ownerSession = signup("offer_owner_auth");
		UUID taskId = createTask(ownerSession, "Auth task", "Task used for auth checks.", "100.00");

		mockMvc.perform(post("/api/tasks/" + taskId + "/offers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(offerBody("50.00", "I can do this quickly.")))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/tasks/" + taskId + "/offers"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void offerCreateListAcceptAndWithdrawFlowWorks() throws Exception {
		MockHttpSession ownerSession = signup("offer_owner_main");
		MockHttpSession sellerOneSession = signup("offer_seller_one");
		MockHttpSession sellerTwoSession = signup("offer_seller_two");
		MockHttpSession sellerThreeSession = signup("offer_seller_three");

		UUID taskId = createTask(ownerSession, "Landing page build", "Build a landing page for launch.", "400.00");

		UUID sellerOneOfferId =
				createOffer(taskId, sellerOneSession, "320.00", "I can ship this in three days.");
		UUID sellerTwoOfferId =
				createOffer(taskId, sellerTwoSession, "300.00", "Happy to provide two revisions.");
		UUID sellerThreeOfferId =
				createOffer(taskId, sellerThreeSession, "280.00", "Can start immediately and provide updates.");

		mockMvc.perform(get("/api/tasks/" + taskId + "/offers").session(ownerSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));

		mockMvc.perform(get("/api/tasks/" + taskId + "/offers").session(sellerOneSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(sellerOneOfferId.toString()));

		mockMvc.perform(get("/api/tasks/" + taskId + "/offers").session(signup("offer_other_user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(post("/api/tasks/" + taskId + "/offers/" + sellerTwoOfferId + "/accept")
						.session(ownerSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACCEPTED"))
				.andExpect(jsonPath("$.id").value(sellerTwoOfferId.toString()));

		mockMvc.perform(get("/api/tasks/" + taskId + "/offers").session(ownerSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id=='" + sellerTwoOfferId + "')].length()").value(1))
				.andExpect(jsonPath("$[?(@.id=='" + sellerTwoOfferId + "')][0].status").value("ACCEPTED"))
				.andExpect(jsonPath("$[?(@.id=='" + sellerOneOfferId + "')][0].status").value("REJECTED"))
				.andExpect(jsonPath("$[?(@.id=='" + sellerThreeOfferId + "')][0].status").value("REJECTED"));

		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id=='" + taskId + "')].length()").value(0));

		mockMvc.perform(get("/api/jobs").session(ownerSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(taskId.toString()))
				.andExpect(jsonPath("$[0].status").value("ASSIGNED"))
				.andExpect(jsonPath("$[0].acceptedOffer.id").value(sellerTwoOfferId.toString()))
				.andExpect(jsonPath("$[0].acceptedOffer.seller.username").value("offer_seller_two"))
				.andExpect(jsonPath("$[0].acceptedOffer.amount").value(300.00));

		mockMvc.perform(get("/api/jobs").session(sellerTwoSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(taskId.toString()))
				.andExpect(jsonPath("$[0].acceptedOffer.id").value(sellerTwoOfferId.toString()));

		mockMvc.perform(get("/api/jobs").session(sellerOneSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		org.assertj.core.api.Assertions.assertThat(transactionRepository.countByOfferId(sellerTwoOfferId))
				.isEqualTo(1L);
	}

	@Test
	void offerValidationAndPermissionRulesAreHandled() throws Exception {
		MockHttpSession ownerSession = signup("offer_owner_rules");
		MockHttpSession sellerSession = signup("offer_seller_rules");
		MockHttpSession otherSession = signup("offer_other_rules");

		UUID taskId = createTask(ownerSession, "Logo refresh", "Refresh the existing brand logo assets.", "250.00");

		mockMvc.perform(post("/api/tasks/" + taskId + "/offers")
						.session(ownerSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(offerBody("200.00", "Owner should not be able to offer.")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN_ACTION"));

		mockMvc.perform(post("/api/tasks/" + taskId + "/offers")
						.session(sellerSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(offerBody("0.00", "Bad amount.")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		UUID offerId = createOffer(taskId, sellerSession, "220.00", "I will provide editable source files.");

		mockMvc.perform(post("/api/tasks/" + taskId + "/offers")
						.session(sellerSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(offerBody("210.00", "Duplicate should fail.")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_OFFER"));

		mockMvc.perform(post("/api/tasks/" + taskId + "/offers/" + offerId + "/accept")
						.session(otherSession))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN_ACTION"));

		mockMvc.perform(post("/api/tasks/" + taskId + "/offers/" + offerId + "/withdraw")
						.session(otherSession))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN_ACTION"));

		mockMvc.perform(post("/api/tasks/" + taskId + "/offers/" + offerId + "/withdraw")
						.session(sellerSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("WITHDRAWN"));

		mockMvc.perform(post("/api/tasks/" + taskId + "/offers/" + offerId + "/withdraw")
						.session(sellerSession))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_OFFER_STATE"));

		mockMvc.perform(post("/api/tasks/" + taskId + "/offers/" + offerId + "/accept")
						.session(ownerSession))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_OFFER_STATE"));
	}

	private MockHttpSession signup(String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(authBody(username, "password123")))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private String authBody(String username, String password) {
		return """
				{"username":"%s","password":"%s"}
				""".formatted(username, password);
	}

	private String taskBody(String title, String description, String budgetAmount) {
		return """
				{"title":"%s","description":"%s","budgetAmount":%s,"budgetCurrency":"USD"}
				""".formatted(title, description, budgetAmount);
	}

	private String offerBody(String amount, String message) {
		return """
				{"amount":%s,"currency":"USD","message":"%s"}
				""".formatted(amount, message);
	}

	private UUID createTask(MockHttpSession session, String title, String description, String budgetAmount)
			throws Exception {
		MvcResult result = mockMvc.perform(post("/api/tasks")
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(taskBody(title, description, budgetAmount)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(node.get("id").asText());
	}

	private UUID createOffer(UUID taskId, MockHttpSession session, String amount, String message)
			throws Exception {
		MvcResult result = mockMvc.perform(post("/api/tasks/" + taskId + "/offers")
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(offerBody(amount, message)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(node.get("id").asText());
	}

}
