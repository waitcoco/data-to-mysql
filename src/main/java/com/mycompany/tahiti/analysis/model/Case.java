package com.mycompany.tahiti.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Case {
    private String caseId;
    private String caseName;
    private String caseType;
    private List<String> suspects;
    private Integer biluNumber;
    private List<Person> detailedPersons = new ArrayList<>();
    private List<Bilu> bilus = new ArrayList<>();
}
