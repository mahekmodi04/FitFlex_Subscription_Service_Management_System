package com.fit.subscription.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.subscription.entity.AddOn;
import com.fit.subscription.repository.AddOnRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AddOnIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AddOnRepository addOnRepository;

    @Test
    @DisplayName("Integration - Create AddOn")
    void createAddOn_ShouldSaveInDatabase() throws Exception {

        addOnRepository.deleteAll();

        String json = """
                {
                    "name":"Protein Shake",
                    "description":"Extra Protein",
                    "unitName":"Bottle",
                    "unitPrice":250,
                    "active":true
                }
                """;

        mockMvc.perform(post("/addons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Protein Shake"))
                .andExpect(jsonPath("$.unitName").value("Bottle"));

        AddOn saved =
                addOnRepository.findByNameIgnoreCaseAndActiveTrue("Protein Shake")
                        .orElse(null);

        assertNotNull(saved);
        assertEquals("Bottle", saved.getUnitName());
        assertEquals(0, BigDecimal.valueOf(250).compareTo(saved.getUnitPrice()));
    }

    @Test
    @DisplayName("Integration - Get All Active AddOns")
    void getAllActiveAddOns_ShouldReturnList() throws Exception {

        addOnRepository.deleteAll();

        AddOn addOn = new AddOn();
        addOn.setName("Protein");
        addOn.setDescription("Extra");
        addOn.setUnitName("Bottle");
        addOn.setUnitPrice(BigDecimal.valueOf(150));
        addOn.setActive(true);

        addOnRepository.save(addOn);

        mockMvc.perform(get("/addons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Protein"));
    }

    @Test
    @DisplayName("Integration - Duplicate AddOn")
    void createDuplicateAddOn_ShouldReturn400() throws Exception {

        addOnRepository.deleteAll();

        AddOn addOn = new AddOn();
        addOn.setName("Protein");
        addOn.setDescription("Extra");
        addOn.setUnitName("Bottle");
        addOn.setUnitPrice(BigDecimal.valueOf(100));
        addOn.setActive(true);

        addOnRepository.save(addOn);

        String json = """
                {
                    "name":"Protein",
                    "description":"Duplicate",
                    "unitName":"Bottle",
                    "unitPrice":100,
                    "active":true
                }
                """;

        mockMvc.perform(post("/addons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Integration - Validation Failure")
    void createAddOn_WithBlankName_ShouldReturn400() throws Exception {

        String json = """
                {
                    "name":"",
                    "description":"Extra",
                    "unitName":"Bottle",
                    "unitPrice":100,
                    "active":true
                }
                """;

        mockMvc.perform(post("/addons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

}
