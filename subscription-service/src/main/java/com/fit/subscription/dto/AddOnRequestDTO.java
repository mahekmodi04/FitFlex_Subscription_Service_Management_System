package com.fit.subscription.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddOnRequestDTO {

    @NotNull
    private Long addOnId;

    @NotNull
    @Min(1)
    private Integer unitsIncluded;
}
