package com.loopers.application.product.dto;

public class ProductCommand {

    public record Register(Long brandId, String name, int price, int stock) {}

    public record Update(String name, int price, int stock) {}
}
