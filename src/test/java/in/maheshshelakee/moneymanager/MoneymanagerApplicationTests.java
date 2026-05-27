package in.maheshshelakee.moneymanager;

import org.junit.jupiter.api.Test;
class MoneymanagerApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void generateBcryptHash() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String hash = encoder.encode("Mahesh@3459");
        System.out.println("BCRYPT_HASH_START:" + hash + ":BCRYPT_HASH_END");
    }
}
