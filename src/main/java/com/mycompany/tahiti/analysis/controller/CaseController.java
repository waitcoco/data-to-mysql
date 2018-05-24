package com.mycompany.tahiti.analysis.controller;

import com.mycompany.tahiti.analysis.configuration.Configs;
import com.mycompany.tahiti.analysis.jena.JenaLibrary;
import com.mycompany.tahiti.analysis.model.Case;
import com.mycompany.tahiti.analysis.model.Person;
import io.swagger.annotations.Api;
import lombok.val;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/cases")
@Api(description = "case controller")
public class CaseController {
    @Autowired
    JenaLibrary jenaLibrary;

    Model model = jenaLibrary.getModel(Configs.getConfig("jenaMappingModel"));

    @GetMapping
    public List<Case> getCases(){
        val list = new ArrayList<Case>();

        val iterator = jenaLibrary.getStatementsByEntityType(model, "gongan:gongan.case");

        while(iterator.hasNext())
        {
            Statement statement = iterator.next();
            Resource resource = statement.getResource();

            Case aCase = new Case();
            val nameIter = jenaLibrary.getStatementsBySP(model, resource, "common:type.object.name");
            while(nameIter.hasNext())
            {

            }
        }

//
//        aCase.setCaseId("1111111122233");
//        aCase.setCaseName("王大锤殴打别人案件");
//        aCase.setBiluNumber(10);
//        aCase.setCaseType("殴打类");
//        aCase.setSuspects(Arrays.asList(new String[]{"王大锤"}));
//        list.add(aCase);
        return list;
    }

    @ResponseBody
    @GetMapping("/{caseId}")
    public Case getCaseById(@PathVariable("caseId") String caseId) {
        Case aCase = new Case();
        aCase.setCaseId("1111111122233");
        aCase.setCaseName("王大锤殴打别人案件");
        aCase.setBiluNumber(10);
        aCase.setSuspects(Arrays.asList(new String[]{"王大锤"}));
        aCase.setCaseType("殴打类");
        Person person = new Person();
        person.setName("王大锤");
        person.setIdentity("32212324324235331X");
        person.setId("http://mycompany.ai.com/person/王大锤");
        person.setBirthDay("1988年6月14日");
        person.setGender("男");
        person.setPhone("18888888881");
        person.setRole("嫌疑人");
        aCase.getDetailedPersons().add(person);
        return aCase;
    }

    @ResponseBody
    @GetMapping("/{caseId}/persons/{personId}")
    public Person getPersonById(@PathVariable("caseId") String caseId, @PathVariable("personId") String personId) {
        Person person = new Person();
        person.setName("王大锤");
        person.setIdentity("32212324324235331X");
        person.setId("http://mycompany.ai.com/person/王大锤");
        person.setBirthDay("1988年6月14日");
        person.setGender("男");
        person.setPhone("18888888881");
        person.setRole("嫌疑人");
        return person;
    }
}
