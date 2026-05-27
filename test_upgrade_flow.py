import requests
import random
import sys

base_url = "http://localhost:8080/api/v1.0"
admin_email = "shelakemahesh024@gmail.com"
admin_password = "Mahesh@3459"

def log_test_result(name, success, message=""):
    status = "SUCCESS" if success else "FAILED"
    print(f"[{status}] {name} - {message}")
    if not success:
        sys.exit(1)

def run_tests():
    print("=== Starting Manual UPI Upgrade Flow Integration Tests ===")

    # 1. Admin Login
    print("\n1. Logging in as Admin...")
    admin_login_resp = requests.post(f"{base_url}/login", json={
        "emailOrPhone": admin_email,
        "password": admin_password
    })
    
    log_test_result("Admin Login", admin_login_resp.status_code == 200, f"Status code: {admin_login_resp.status_code}")
    admin_token = admin_login_resp.json()["data"]["token"]
    admin_headers = {"Authorization": f"Bearer {admin_token}"}

    # 2. Register a new test user
    print("\n2. Registering a new test user...")
    test_user_email = f"testpro_{random.randint(1000, 9999)}@gmail.com"
    test_user_phone = f"+9199999{random.randint(10000, 99999)}"
    test_user_password = "TestPassword123"
    
    register_payload = {
        "fullName": "Test Pro User",
        "email": test_user_email,
        "phoneNumber": test_user_phone,
        "password": test_user_password
    }
    
    register_resp = requests.post(f"{base_url}/register", json=register_payload)
    log_test_result("Register User", register_resp.status_code == 201, f"Registered: {test_user_email}, status: {register_resp.status_code}")
    
    user_id = register_resp.json()["data"]["id"]

    # 3. Verify and Activate user using Admin API
    print("\n3. Verifying and activating user via Admin endpoints...")
    verify_resp = requests.put(f"{base_url}/admin/users/{user_id}/verify", headers=admin_headers)
    log_test_result("Admin Toggle Verification", verify_resp.status_code == 200, f"Verified badge toggled: {verify_resp.status_code}")

    # 4. User Login
    print("\n4. Logging in as new test user...")
    user_login_resp = requests.post(f"{base_url}/login", json={
        "emailOrPhone": test_user_email,
        "password": test_user_password
    })
    log_test_result("User Login", user_login_resp.status_code == 200, f"Logged in: {test_user_email}, status: {user_login_resp.status_code}")
    
    user_token = user_login_resp.json()["data"]["token"]
    user_headers = {"Authorization": f"Bearer {user_token}"}

    # 5. Fetch initial subscription status
    print("\n5. Checking initial subscription status (should be NONE)...")
    status_resp = requests.get(f"{base_url}/subscriptions/my-status", headers=user_headers)
    log_test_result("Initial Subscription Status", status_resp.status_code == 200, f"Status: {status_resp.json()['data']['status']}")
    
    initial_status = status_resp.json()["data"]["status"]
    log_test_result("Initial Status is NONE", initial_status == "NONE", f"Expected: NONE, Got: {initial_status}")

    # 6. Submit upgrade request
    print("\n6. Submitting manual UPI upgrade request...")
    txn_id = f"TXN{random.randint(100000000000, 999999999999)}"
    upgrade_payload = {
        "planType": "MONTHLY",
        "transactionId": txn_id
    }
    upgrade_resp = requests.post(f"{base_url}/subscriptions/upgrade", json=upgrade_payload, headers=user_headers)
    log_test_result("Submit Upgrade Request", upgrade_resp.status_code == 200, f"Submitted request, status: {upgrade_resp.status_code}")

    # 7. Test Duplicate Transaction ID check
    print("\n7. Testing duplicate Transaction ID validation (using a different user)...")
    test_user_email2 = f"testpro_dup_{random.randint(1000, 9999)}@gmail.com"
    test_user_phone2 = f"+9199999{random.randint(10000, 99999)}"
    register_payload2 = {
        "fullName": "Test Pro User 2",
        "email": test_user_email2,
        "phoneNumber": test_user_phone2,
        "password": test_user_password
    }
    register_resp2 = requests.post(f"{base_url}/register", json=register_payload2)
    user_id2 = register_resp2.json()["data"]["id"]
    
    # Verify 2nd user
    requests.put(f"{base_url}/admin/users/{user_id2}/verify", headers=admin_headers)
    
    # Login 2nd user
    user_login_resp2 = requests.post(f"{base_url}/login", json={
        "emailOrPhone": test_user_email2,
        "password": test_user_password
    })
    user_token2 = user_login_resp2.json()["data"]["token"]
    user_headers2 = {"Authorization": f"Bearer {user_token2}"}
    
    # Try to submit the same transaction ID
    dup_resp = requests.post(f"{base_url}/subscriptions/upgrade", json=upgrade_payload, headers=user_headers2)
    log_test_result("Duplicate Transaction ID Blocked", dup_resp.status_code == 409, f"Received status code: {dup_resp.status_code}")

    # 8. Test Multiple Pending requests check
    print("\n8. Testing block on repeated pending submissions...")
    new_txn_id = f"TXN{random.randint(100000000000, 999999999999)}"
    repeat_payload = {
        "planType": "MONTHLY",
        "transactionId": new_txn_id
    }
    repeat_resp = requests.post(f"{base_url}/subscriptions/upgrade", json=repeat_payload, headers=user_headers)
    log_test_result("Repeated Pending Request Blocked", repeat_resp.status_code == 400, f"Received status code: {repeat_resp.status_code}")

    # 9. Verify status is PENDING
    print("\n9. Verifying subscription status has updated to PENDING...")
    pending_status_resp = requests.get(f"{base_url}/subscriptions/my-status", headers=user_headers)
    p_status = pending_status_resp.json()["data"]["status"]
    log_test_result("Status updated to PENDING", p_status == "PENDING", f"Expected: PENDING, Got: {p_status}")

    # 10. Fetch pending requests as Admin
    print("\n10. Fetching pending manual requests as Admin...")
    admin_req_resp = requests.get(f"{base_url}/admin/subscriptions/requests", headers=admin_headers)
    log_test_result("Admin Get Pending Requests", admin_req_resp.status_code == 200, f"Status: {admin_req_resp.status_code}")
    
    requests_list = admin_req_resp.json()["data"]
    matched_req = [r for r in requests_list if r["transactionId"] == txn_id]
    log_test_result("Pending request listed for Admin", len(matched_req) > 0, f"Found matched request: {len(matched_req) > 0}")
    
    manual_sub_id = matched_req[0]["id"]

    # 11. Approve request as Admin
    print("\n11. Approving manual upgrade request as Admin...")
    approve_resp = requests.post(f"{base_url}/admin/subscriptions/requests/{manual_sub_id}/approve", headers=admin_headers)
    log_test_result("Admin Approve Request", approve_resp.status_code == 200, f"Approved subscription, status: {approve_resp.status_code}")

    # 12. Verify user has updated role
    print("\n12. Verifying user role updated to PRO...")
    profile_resp = requests.get(f"{base_url}/profile", headers=user_headers)
    user_role = profile_resp.json()["data"]["role"]
    log_test_result("User role updated to PRO", user_role == "PRO", f"Expected: PRO, Got: {user_role}")

    # 13. Verify subscription is APPROVED
    print("\n13. Checking final subscription status...")
    final_status_resp = requests.get(f"{base_url}/subscriptions/my-status", headers=user_headers)
    f_status = final_status_resp.json()["data"]["status"]
    remaining_days = final_status_resp.json()["data"]["remainingDays"]
    log_test_result("Status updated to APPROVED", f_status == "APPROVED", f"Expected: APPROVED, Got: {f_status}")
    log_test_result("Remaining days is valid", remaining_days >= 28, f"Remaining days: {remaining_days}")

    print("\n=== All Integration Tests Completed Successfully! ===")

if __name__ == "__main__":
    run_tests()
