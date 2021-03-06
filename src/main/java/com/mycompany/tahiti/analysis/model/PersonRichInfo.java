package com.mycompany.tahiti.analysis.model;

import com.mycompany.tahiti.analysis.repository.CaseBaseInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PersonRichInfo extends PersonModel{
    List<InvolvedCaseWithRole> involvedCases = new ArrayList<>();
    List<SameCasePerson> sameCasePersonList = new ArrayList<>();
    List<SameCaseEntity> sameCasePersonNameList = new ArrayList<>();

    @Data
    public class InvolvedCaseWithRole extends CaseBaseInfo {
        String role;
    }

    @Data
    public class SameCasePerson extends PersonModel{
        List<CaseBaseInfo> sameCases;

        public boolean equals(Object obj){
            if(this.getIdentity()!=null&&((SameCasePerson)obj).getIdentity()!=null&&this.getIdentity().equals(((SameCasePerson)obj).getIdentity())||this.getName()!=null&&((SameCasePerson)obj).getName()!=null&&this.getName().equals(((SameCasePerson)obj).getName())){
                return true;
            }
            return false;
        }
    }

    @Data
    public class SameCaseEntity extends ValueObject{
        List<CaseBaseInfo> sameCases;
        String subjectId;

        public SameCaseEntity(String value) {
            super(value);
        }

        public boolean equals(Object obj){
            if(this.getValue()!= null && ((SameCaseEntity)obj).getValue()!=null && this.getValue().equals(((SameCaseEntity) obj).getValue()))return true;
            return false;
        }
    }
}
