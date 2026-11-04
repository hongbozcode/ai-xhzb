package com.xhzb.nursing.domain.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/** 入住家属信息。 */
@Data
public class ElderFamilyDto {
    /** 家属姓名 */
    @NotBlank
    @Size(max = 50)
    private String name;
    /** 家属联系电话 */
    @NotBlank
    @Size(max = 20)
    private String phone;
    /** 亲属关系 */
    @NotBlank
    @Size(max = 20)
    private String kinship;
}
