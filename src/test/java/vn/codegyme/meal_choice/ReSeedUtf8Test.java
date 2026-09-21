package vn.codegyme.meal_choice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

@SpringBootTest
public class ReSeedUtf8Test {

    @Autowired
    private DataSource dataSource;

    @Test
    void runUtf8Seed() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("SET NAMES utf8mb4;");
            }

            File seedFile = new File("database/seed.sql");
            File seedExtraFile = new File("database/seed_extra.sql");

            System.out.println(">>> 1. Re-executing seed.sql with UTF-8...");
            ScriptUtils.executeSqlScript(conn, new EncodedResource(new FileSystemResource(seedFile), StandardCharsets.UTF_8));
            System.out.println(">>> seed.sql executed with UTF-8!");

            System.out.println(">>> 2. Re-executing seed_extra.sql with UTF-8...");
            ScriptUtils.executeSqlScript(conn, new EncodedResource(new FileSystemResource(seedExtraFile), StandardCharsets.UTF_8));
            System.out.println(">>> seed_extra.sql executed with UTF-8!");

            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT name FROM foods LIMIT 5");
            while (rs.next()) {
                System.out.println(">>> Sample food name: " + rs.getString("name"));
            }
        }
    }
}
