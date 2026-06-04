package in.maheshshelakee.moneymanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.maheshshelakee.moneymanager.dto.*;
import in.maheshshelakee.moneymanager.entity.Role;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SubscriptionUpgradeFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private static final String ADMIN_EMAIL = "shelakemahesh024@gmail.com";
    private static final String ADMIN_PASSWORD = "Mahesh@3459";
    private static final String CONTEXT_PATH = "/api/v1.0";

    @BeforeEach
    public void setup() throws Exception {
        // Ensure admin user exists and is configured correctly
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElse(null);
        if (admin == null) {
            admin = User.builder()
                    .fullName("Admin")
                    .email(ADMIN_EMAIL)
                    .phoneNumber("+919876543210")
                    .password(passwordEncoder.encode(ADMIN_PASSWORD))
                    .role(Role.ADMIN)
                    .isActive(true)
                    .isVerified(true)
                    .build();
            userRepository.save(admin);
        } else {
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setRole(Role.ADMIN);
            admin.setIsActive(true);
            admin.setIsVerified(true);
            userRepository.save(admin);
        }

        // Login as admin
        LoginRequest adminLogin = new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD);
        MvcResult result = mockMvc.perform(post(CONTEXT_PATH + "/login")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseStr, Map.class);
        Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
        adminToken = (String) dataMap.get("token");
    }

    @Test
    public void testUPIUpgradeFlow() throws Exception {
        Random random = new Random();
        int suffix = random.nextInt(9000) + 1000;
        String userEmail = "testpro_" + suffix + "@gmail.com";
        String userPhone = "+9199999" + (random.nextInt(90000) + 10000);
        String userPassword = "TestPassword123";

        // 2. Register a new test user
        Map<String, Object> registerPayload = Map.of(
                "fullName", "Test Pro User",
                "email", userEmail,
                "phoneNumber", userPhone,
                "password", userPassword
        );

        MvcResult regResult = mockMvc.perform(post(CONTEXT_PATH + "/register")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        String regResponseStr = regResult.getResponse().getContentAsString();
        Map<String, Object> regResponseMap = objectMapper.readValue(regResponseStr, Map.class);
        Map<String, Object> regDataMap = (Map<String, Object>) regResponseMap.get("data");
        Number userIdNum = (Number) regDataMap.get("id");
        long userId = userIdNum.longValue();

        // 3. Verify and Activate user using Admin API
        mockMvc.perform(put(CONTEXT_PATH + "/admin/users/" + userId + "/verify")
                        .contextPath(CONTEXT_PATH)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 4. User Login
        LoginRequest userLogin = new LoginRequest(userEmail, userPassword);
        MvcResult userLoginResult = mockMvc.perform(post(CONTEXT_PATH + "/login")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String userLoginResponseStr = userLoginResult.getResponse().getContentAsString();
        Map<String, Object> userLoginResponseMap = objectMapper.readValue(userLoginResponseStr, Map.class);
        Map<String, Object> userLoginDataMap = (Map<String, Object>) userLoginResponseMap.get("data");
        String userToken = (String) userLoginDataMap.get("token");
        String userHeadersAuth = "Bearer " + userToken;

        // 5. Fetch initial subscription status (should be NONE)
        mockMvc.perform(get(CONTEXT_PATH + "/subscriptions/my-status")
                        .contextPath(CONTEXT_PATH)
                        .header("Authorization", userHeadersAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("NONE")));

        // 6. Submit upgrade request
        String txnId = "TXN" + (random.nextLong(900000000000L) + 100000000000L);
        SubscriptionRequestDTO upgradePayload = new SubscriptionRequestDTO();
        upgradePayload.setPlanType("MONTHLY");
        upgradePayload.setTransactionId(txnId);

        mockMvc.perform(post(CONTEXT_PATH + "/subscriptions/upgrade")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upgradePayload))
                        .header("Authorization", userHeadersAuth))
                .andExpect(status().isOk());

        // 7. Test Duplicate Transaction ID check
        String userEmail2 = "testpro_dup_" + suffix + "@gmail.com";
        String userPhone2 = "+9199999" + (random.nextInt(90000) + 10000);
        Map<String, Object> registerPayload2 = Map.of(
                "fullName", "Test Pro User 2",
                "email", userEmail2,
                "phoneNumber", userPhone2,
                "password", userPassword
        );

        MvcResult regResult2 = mockMvc.perform(post(CONTEXT_PATH + "/register")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload2)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> regResponseMap2 = objectMapper.readValue(regResult2.getResponse().getContentAsString(), Map.class);
        long userId2 = ((Number) ((Map<String, Object>) regResponseMap2.get("data")).get("id")).longValue();

        // Verify user 2
        mockMvc.perform(put(CONTEXT_PATH + "/admin/users/" + userId2 + "/verify")
                        .contextPath(CONTEXT_PATH)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Login user 2
        LoginRequest userLogin2 = new LoginRequest(userEmail2, userPassword);
        MvcResult userLoginResult2 = mockMvc.perform(post(CONTEXT_PATH + "/login")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin2)))
                .andExpect(status().isOk())
                .andReturn();

        String userToken2 = (String) ((Map<String, Object>) objectMapper.readValue(userLoginResult2.getResponse().getContentAsString(), Map.class).get("data")).get("token");

        // Try to submit duplicate txn ID
        mockMvc.perform(post(CONTEXT_PATH + "/subscriptions/upgrade")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upgradePayload))
                        .header("Authorization", "Bearer " + userToken2))
                .andExpect(status().isConflict());

        // 8. Test Multiple Pending requests check
        String newTxnId = "TXN" + (random.nextLong(900000000000L) + 100000000000L);
        SubscriptionRequestDTO repeatPayload = new SubscriptionRequestDTO();
        repeatPayload.setPlanType("MONTHLY");
        repeatPayload.setTransactionId(newTxnId);

        mockMvc.perform(post(CONTEXT_PATH + "/subscriptions/upgrade")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repeatPayload))
                        .header("Authorization", userHeadersAuth))
                .andExpect(status().isBadRequest());

        // 9. Verify status is PENDING
        mockMvc.perform(get(CONTEXT_PATH + "/subscriptions/my-status")
                        .contextPath(CONTEXT_PATH)
                        .header("Authorization", userHeadersAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING")));

        // 10. Fetch pending requests as Admin
        MvcResult pendingReqsResult = mockMvc.perform(get(CONTEXT_PATH + "/admin/subscriptions/requests")
                        .contextPath(CONTEXT_PATH)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String pendingReqsStr = pendingReqsResult.getResponse().getContentAsString();
        Map<String, Object> pendingReqsMap = objectMapper.readValue(pendingReqsStr, Map.class);
        List<Map<String, Object>> requestsList = (List<Map<String, Object>>) pendingReqsMap.get("data");

        Map<String, Object> matchedReq = null;
        for (Map<String, Object> req : requestsList) {
            if (txnId.equals(req.get("transactionId"))) {
                matchedReq = req;
                break;
            }
        }
        assertNotNull(matchedReq, "Pending request should be listed for Admin");
        long manualSubId = ((Number) matchedReq.get("id")).longValue();

        // 11. Approve request as Admin
        mockMvc.perform(post(CONTEXT_PATH + "/admin/subscriptions/requests/" + manualSubId + "/approve")
                        .contextPath(CONTEXT_PATH)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 12. Verify user has updated role
        mockMvc.perform(get(CONTEXT_PATH + "/profile")
                        .contextPath(CONTEXT_PATH)
                        .header("Authorization", userHeadersAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role", is("PRO")));

        // 13. Verify subscription is APPROVED
        mockMvc.perform(get(CONTEXT_PATH + "/subscriptions/my-status")
                        .contextPath(CONTEXT_PATH)
                        .header("Authorization", userHeadersAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")))
                .andExpect(jsonPath("$.data.remainingDays", greaterThanOrEqualTo(28)));
    }
}
