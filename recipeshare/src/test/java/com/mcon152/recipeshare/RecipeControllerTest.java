package com.mcon152.recipeshare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcon152.recipeshare.web.RecipeController;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private static ObjectMapper mapper;

    @BeforeAll
    static void setup() {
        mapper = new ObjectMapper();
    }

    // Internal class for creation-related tests
    @Nested
    @DisplayName("Tests for creating and adding recipes")
    class CreationTests {
        @Test
        @DisplayName("Test addRecipe(): Create json string, add to recipes list, check if values are as expected")
        void testAddRecipe() throws Exception {

            ObjectNode json = mapper.createObjectNode();
            json.put("title", "Cake");
            json.put("description", "Delicious cake");
            // Change ingredients to a single String
            json.put("ingredients", "1 cup of flour, 1 cup of sugar, 3 eggs");
            json.put("instructions", "Mix and bake");
            String jsonString = mapper.writeValueAsString(json);
            mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonString))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Cake"))
                    .andExpect(jsonPath("$.description").value("Delicious cake"))
                    .andExpect(jsonPath("$.ingredients").value("1 cup of flour, 1 cup of sugar, 3 eggs"))
                    .andExpect(jsonPath("$.instructions").value("Mix and bake"))
                    .andExpect(jsonPath("$.id").isNumber());
        }

        @ParameterizedTest
        @DisplayName("Test addRecipe() with parameters: Create json strings and add to recipes list, check that values are as expected")
        @CsvSource({
                "'Chocolate Cake','Rich chocolate cake','2 cups flour;1 cup cocoa;4 eggs','Bake at 350F for 30 min'",
                "'Pasta Salad','Fresh pasta salad','200g pasta;100g tomatoes;50g olives','Mix all ingredients'",
                "'Pancakes','Fluffy pancakes','1 cup flour;2 eggs;1 cup milk','Cook on skillet until golden'"
        })
        void parameterizedAddRecipeTest(String title, String description, String ingredients, String instructions) throws Exception {
            ObjectNode json = mapper.createObjectNode();
            json.put("title", title);
            json.put("description", description);
            json.put("ingredients", ingredients);
            json.put("instructions", instructions);
            String jsonString = mapper.writeValueAsString(json);
            mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonString))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value(title))
                    .andExpect(jsonPath("$.description").value(description))
                    .andExpect(jsonPath("$.ingredients").value(ingredients))
                    .andExpect(jsonPath("$.instructions").value(instructions))
                    .andExpect(jsonPath("$.id").isNumber());
        }
    }

    // Internal class for delete and get tests
    @Nested
    @DisplayName("Tests for CRUD methods")
    class DeleteAndGetTests {
        private List<Integer> recipeIds;

        @BeforeEach
        @DisplayName("Create and populate recipes Array and recipeIds ArrayList")
        void createRecipes() throws Exception {
            recipeIds = new ArrayList<>();
            String[] recipes = {
                    "{\"title\":\"Pie\",\"description\":\"Apple pie\",\"ingredients\":\"Apples, Flour, Sugar\",\"instructions\":\"Mix and bake\"}",
                    "{\"title\":\"Soup\",\"description\":\"Tomato soup\",\"ingredients\":\"Tomatoes, Water, Salt\",\"instructions\":\"Boil and blend\"}"
            };
            for (String json : recipes) {
                String response = mockMvc.perform(post("/api/recipes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
                int id = mapper.readTree(response).get("id").asInt();
                recipeIds.add(id);
            }
        }

        @Test
        @DisplayName("Test getAllRecipes(), check titles of all strings")
        void testGetAllRecipes() throws Exception {
            mockMvc.perform(get("/api/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Pie"))
                    .andExpect(jsonPath("$[1].title").value("Soup"));
        }

        @Test
        @DisplayName("Test getRecipe(), check that title is as expected")
        void testGetRecipe() throws Exception {
            int id = recipeIds.get(0);
            mockMvc.perform(get("/api/recipes/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Pie"));
        }

        @Test
        @DisplayName("Test deleteRecipe(), check that recipe can't be found after deletion")
        void testDeleteRecipe() throws Exception {
            int id = recipeIds.get(0);
            mockMvc.perform(delete("/api/recipes/" + id)).andExpect(status().isNoContent());
            mockMvc.perform(get("/api/recipes/" + id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Test putRecipe(), check that values change")
        void testPutRecipe() throws Exception {
            int id = recipeIds.get(0);
            mockMvc.perform(put("/api/recipes/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(("{\"id\":%d,\"title\":\"Sandwich\",\"description\":\"PB&J Sandwich\",\"ingredients\":" +
                            "\"Bread, Peanut Butter, Jelly\",\"instructions\":\"Spread, close, eat.\"}").formatted(id)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/recipes/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Sandwich"))
                    .andExpect(jsonPath("$.description").value("PB&J Sandwich"));
        }

        @Test
        @DisplayName("Test patchRecipe(), check that values change or stay the same as expected")
        void testPatchRecipe() throws Exception {
            int id = recipeIds.get(0);
            mockMvc.perform(patch("/api/recipes/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ingredients\":\"Flour, butter, apples, brown sugar\",\"instructions\":\"Make crust, add filling, bake\"}"))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/recipes/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Pie"))
                    .andExpect(jsonPath("$.description").value("Apple pie"))
                    .andExpect(jsonPath("$.ingredients").value("Flour, butter, apples, brown sugar"))
                    .andExpect(jsonPath("$.instructions").value("Make crust, add filling, bake"));
        }
    }

    @Nested
    @DisplayName("Tests for calls on nonexistent ids")
    class NonExistingRecipeTests {

        @Test
        @DisplayName("Test getRecipe() for nonexistent id, expecting 404")
        void testGetNonExistingRecipe() throws Exception {
            mockMvc.perform(get("/api/recipes/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Test putRecipe() for nonexistent id, expecting 404")
        void testPutNonExistingRecipe() throws Exception {
            mockMvc.perform(put("/api/recipes/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(("{\"id\":999,\"title\":\"Sandwich\",\"description\":\"PB&J Sandwich\",\"ingredients\":" +
                                    "\"Bread, Peanut Butter, Jelly\",\"instructions\":\"Spread, close, eat.\"}")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Test patchRecipe() for nonexistent id, expecting 404")
        void testPatchNonExistingRecipe() throws Exception {
            mockMvc.perform(patch("/api/recipes/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ingredients\":\"Flour, butter, apples, brown sugar\",\"instructions\":\"Make crust, add filling, bake\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Test deleteRecipe() for nonexistent id, expecting 404")
        void testDeleteNonExistingRecipe() throws Exception {
            mockMvc.perform(delete("/api/recipes/999")).andExpect(status().isNotFound());
        }
    }



}