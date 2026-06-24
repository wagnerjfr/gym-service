package com.demo.gym.common;

import jakarta.persistence.Embeddable;

@Embeddable
public record Address(String country, String city, String street, String zipCode) {}
