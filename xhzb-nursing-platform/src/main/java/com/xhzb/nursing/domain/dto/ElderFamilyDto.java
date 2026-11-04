package com.xhzb.nursing.domain.dto;

import jakarta.validation.constraints.*;

/** 入住家属信息。 */
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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getKinship() { return kinship; }
    public void setKinship(String kinship) { this.kinship = kinship; }
}
