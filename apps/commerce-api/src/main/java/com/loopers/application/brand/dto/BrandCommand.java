package com.loopers.application.brand.dto;

public class BrandCommand {

    public record Register(String name) {
    }

    public record Update(String name) {
    }
}
