package com.tsp.jimi_api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

/**
 * Returns index web page (required for Azure Container Webapps).
 */
@RestController
public class BaseController {

    /**
     * Gets image as response entity.
     *
     * @return JIMI logo if server is up.
     * @throws IOException if file is not correctly read.
     */
    @GetMapping(value = "/", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get server status")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Server is up", content = {@Content(mediaType = "image/png")}),
            @ApiResponse(responseCode = "503", description = "Server is down", content = @Content)})
    public ResponseEntity<byte[]> getImageAsResponseEntity() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        InputStream in = new ClassPathResource("images/logo.png", getClass().getClassLoader()).getInputStream();
        byte[] media = in.readAllBytes();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        return new ResponseEntity<>(media, headers, HttpStatus.OK);
    }
}

