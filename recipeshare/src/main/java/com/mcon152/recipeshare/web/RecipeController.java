package com.mcon152.recipeshare.web;

import com.mcon152.recipeshare.Recipe;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    private final List<Recipe> recipes = new ArrayList<Recipe>();
    private final AtomicLong counter = new AtomicLong();

    @GetMapping
    public Recipe addRecipe(@RequestBody Recipe recipe) {
        recipe.setId(counter.incrementAndGet(1));

    }
}
