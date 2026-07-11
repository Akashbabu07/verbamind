package com.verbamind.user.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@NotBlank @Size(max = 100,min = 2) String fullName) {

}
