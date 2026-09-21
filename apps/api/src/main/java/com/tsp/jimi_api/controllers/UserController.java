package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.global.Shared;
import com.tsp.jimi_api.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * User-level operations that don't fit into the chat or agenda controllers —
 * for now, just a full data wipe.
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    private static final String DELETE_ERROR = "User data deletion failed.";

    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Delete every piece of data tied to the given user (agenda events + in-progress conversations).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data deleted",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"deleted\":7}"))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid userId",
                    content = @Content(mediaType = "application/json"))})
    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteUserData(@RequestParam("userId") final String userId) {
        if (userId == null || userId.isBlank()) {
            return Shared.raiseError(DELETE_ERROR, "Incorrect or missing user id.", LOGGER);
        }
        try {
            int deleted = userService.deleteAllUserData(userId);
            return ResponseEntity.ok(Map.of("deleted", deleted));
        } catch (Exception e) {
            return Shared.raiseError(DELETE_ERROR, e.getClass().getSimpleName() + ": " + e.getMessage(), LOGGER);
        }
    }
}
