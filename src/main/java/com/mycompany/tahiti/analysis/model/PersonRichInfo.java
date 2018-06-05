package com.mycompany.tahiti.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PersonRichInfo extends PersonModel{
    List<InvolvedCaseWithRole> involvedCases = new ArrayList<>();
    List<SameCasePerson> sameCasePersonList = new ArrayList<>();

    @Data
    public class InvolvedCaseWithRole extends CaseBaseInfo{
        String role;
    }

    @Data
    public class SameCasePerson extends PersonModel{
        int sameCasesCount;
        List<CaseBaseInfo> sameCases;

        public boolean equals(Object obj){
            if(this.getIdentity().equals(((SameCasePerson)obj).getIdentity())||this.getName().equals(((SameCasePerson)obj).getName())){
                return true;
            }
            return false;
        }
    }
}
