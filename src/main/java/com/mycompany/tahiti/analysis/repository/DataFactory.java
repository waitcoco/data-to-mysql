package com.mycompany.tahiti.analysis.repository;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.mycompany.tahiti.analysis.jena.JenaLibrary;
import lombok.val;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class DataFactory {
    @Autowired
    JenaLibrary jenaLibrary;

    // This is only for cache, not full data
    // caseId, Case
    private Map<String, Case> caseCache = new HashMap<>();

    // biluId， Bilu
    private Map<String, Bilu> biluCache = new HashMap<>();

    // subjectId, Person
    private Map<String, Person> personCache = new HashMap<>();

    // This is all cases with SimpleCase
    // subjectId, SimpleCase
    private Map<String, CaseBaseInfo> allSimpleCases = new HashMap<>();

    public boolean clear(){
        caseCache.clear();
        biluCache.clear();
        personCache.clear();
        allSimpleCases.clear();
        getAllCaseBaseInfo();
        return true;
    }

    public Case getCaseById(String caseId) {
        if (caseCache.containsKey(caseId))
            return caseCache.get(caseId);
        else {
            try {
                jenaLibrary.openReadTransaction();
                Model model = jenaLibrary.getRuntimeModel();
                val iterator = jenaLibrary.getStatementsById(model, caseId);

                if (iterator.hasNext()) {
                    Statement statement = iterator.next();
                    Resource resource = statement.getSubject();
                    Case aCase = getCaseInfo(model, resource);
                    caseCache.put(caseId, aCase);
                    return aCase;
                }
                else
                    return new Case();
            } finally {
                jenaLibrary.closeTransaction();
            }
        }
    }

    public Bilu getBiluById(String biluId) {
        if (biluCache.containsKey(biluId))
            return biluCache.get(biluId);
        else {
            try {
                jenaLibrary.openReadTransaction();
                Model model = jenaLibrary.getRuntimeModel();
                val iterator = jenaLibrary.getStatementsById(model, biluId);

                if (iterator.hasNext()) {
                    Statement statement = iterator.next();
                    Resource resource = statement.getSubject();
                    Bilu bilu = getBiluInfo(model, resource);
                    biluCache.put(biluId, bilu);
                    return bilu;
                }
                else
                    return new Bilu();
            } finally {
                jenaLibrary.closeTransaction();
            }
        }
    }

    public Person getPersonById(String pSubjectId) {
        if (personCache.containsKey(pSubjectId))
            return personCache.get(pSubjectId);
        else {
            try {
                jenaLibrary.openReadTransaction();
                Model model = jenaLibrary.getRuntimeModel();

                Person person = getPersonInfo(model, model.getResource(pSubjectId));
                personCache.put(pSubjectId, person);
                return person;
            } finally {
                jenaLibrary.closeTransaction();
            }
        }
    }

    private Person getPersonInfo(Model model, Resource resource) {
        Person person = new Person();

        person.setSubjectId(resource.toString());
        val pNames = jenaLibrary.getStringValueBySP(model, resource, "common:type.object.name");
        if (pNames.size() > 0)
            person.setName(pNames.get(0));

        val personIdentities = jenaLibrary.getStatementsBySP(model, resource, "common:person.person.identification");
        if (personIdentities.hasNext()) {
            val personIds = jenaLibrary.getStringValueBySP(model, personIdentities.next().getResource(), "common:person.identification.number");
            if (personIds.size() > 0)
                person.setIdentity(personIds.get(0));
        }

        val contactIters = jenaLibrary.getStatementsBySP(model, resource, "common:person.person.contact");
        if (contactIters.hasNext()) {
            val contacts = jenaLibrary.getStringValueBySP(model, contactIters.next().getResource(), "common:person.contact.number");
            if (contacts.size() > 0)
                person.setPhone(contacts.get(0));
        }

        val birthdays = jenaLibrary.getStringValueBySP(model, resource, "common:person.person.birthDate");
        if (birthdays.size() > 0)
            person.setBirthDay(birthdays.get(0));

        val genders = jenaLibrary.getStringValueBySP(model, resource, "common:person.person.gender");
        if (genders.size() > 0) {
            if (genders.get(0).toLowerCase().equals("female"))
                person.setGender("女");
            else if (genders.get(0).toLowerCase().equals("male"))
                person.setGender("男");
        }

        // set bilus
        val relatedBilus = Lists.newArrayList(jenaLibrary.getStatementsByPO(model, "gongan:gongan.bilu.entity", resource))
                .stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());

        val biluIds = jenaLibrary.getStringValuesByBatchSP(model, relatedBilus, "common:type.object.id");
        person.setBiluList(biluIds);

        // set cases
        val relatedCases = Lists.newArrayList(jenaLibrary.getStatementsByBatchPO(model, "gongan:gongan.case.bilu", relatedBilus))
                .stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());
        val caseIds = jenaLibrary.getStringValuesByBatchSP(model, relatedCases, "common:type.object.id");
        person.setCaseList(caseIds);

        return person;
    }

    private Bilu getBiluInfo(Model model, Resource resource) {
        Bilu bilu = new Bilu();

        bilu.setSubjectId(resource.toString());

        //bilu id
        List<String> bilu_id_list = jenaLibrary.getStringValueBySP(model, resource, "common:type.object.id");
        bilu.setBiluId(bilu_id_list.size() > 0 ? bilu_id_list.get(0) : "");

        //bilu name
        List<String> bilu_name_list = jenaLibrary.getStringValueBySP(model, resource, "common:type.object.name");
        bilu.setName(bilu_name_list.size() > 0 ? bilu_name_list.get(0) : "");

        //bilu content
        List<String> bilu_content_list = jenaLibrary.getStringValueBySP(model, resource, "common:common.document.contentStream");
        bilu.setContent(bilu_content_list.size() > 0 ? bilu_content_list.get(0) : "");

        // set persons
        val entities = Lists.newArrayList(jenaLibrary.getStatementsBySP(model, resource, "gongan:gongan.bilu.entity")).stream().map(s -> s.getResource().toString()).distinct().collect(Collectors.toList());
        val persons = Lists.newArrayList(jenaLibrary.getStatementsByPOValue(model, "common:type.object.type", "common:person.person")).stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());
        persons.retainAll(entities);

        List<String> biluConnections = Lists.newArrayList(jenaLibrary.getStatementsByPO(model, "common:common.connection.from", resource)).stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());
        for (String personSubject : persons) {
            // set person information
            if(!personCache.containsKey(personSubject))
            {
                Person person = getPersonInfo(model, model.getResource(personSubject));
                personCache.put(personSubject, person);
            }
            bilu.getPersons().add(personCache.get(personSubject));

            // set connection
            List<String> connectionVal = Lists.newArrayList(jenaLibrary.getStatementsByPO(model, "common:common.connection.to", model.getResource(personSubject))).stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());
            connectionVal.retainAll(biluConnections);

            String role = "";
            for (String connection : connectionVal) {
                val connectionType = jenaLibrary.getStringValueBySP(model, model.getResource(connection), "common:common.connection.type");

                if (connectionType.contains("common:common.connection.BiluEntityXianyiren"))
                    role += Role.BiluEntityXianyiren.toString() + "；";
                if (connectionType.contains("common:common.connection.BiluEntityZhengren"))
                    role += Role.BiluEntityZhengren.toString() + "；";
                if (connectionType.contains("common:common.connection.BiluEntityBaoanren"))
                    role += Role.BiluEntityBaoanren.toString() + "；";
                if (connectionType.contains("common:common.connection.BiluEntityDangshiren"))
                    role += Role.BiluEntityDangshiren.toString() + "；";
                if (connectionType.contains("common:common.connection.BiluEntityShouhairen"))
                    role += Role.BiluEntityShouhairen.toString() + "；";
            }

            int roleLength = role.length();
            if (roleLength > 0) {
                bilu.getConnections().put(personSubject, role.substring(0, roleLength - 1));
            }
        }

        // get all things
        val things = Lists.newArrayList(jenaLibrary.getStatementsBySP(model, resource, "gongan:gongan.bilu.thing")).stream().map(s -> s.getResource().toString()).distinct().collect(Collectors.toList());

        // set phone
        val phones = Lists.newArrayList(jenaLibrary.getStatementsByPOValue(model, "common:type.object.type", "common:thing.phone")).stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());
        phones.retainAll(things);
        for (String phone : phones) {
            List<String> phoneNums = jenaLibrary.getStringValueBySP(model, model.getResource(phone), "common:thing.phone.phoneNumber");
            if (phoneNums.size() > 0)
                bilu.getPhones().put(phone, phoneNums.get(0));
        }
        // set bank cards
        val bankCards = Lists.newArrayList(jenaLibrary.getStatementsByPOValue(model, "common:type.object.type", "common:thing.bankcard")).stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());
        bankCards.retainAll(things);
        for (String bankCard : bankCards) {
            List<String> bankCardNums = jenaLibrary.getStringValueBySP(model, model.getResource(bankCard), "common:thing.bankcard.bankCardId");
            if (bankCardNums.size() > 0)
                bilu.getBankCards().put(bankCard, bankCardNums.get(0));
        }

        return bilu;
    }

    private Case getCaseInfo(Model model, Resource resource) {
        Case aCase = new Case();

        if(allSimpleCases.containsKey(resource.toString()))
            aCase.SetBy(allSimpleCases.get(resource.toString()));

        else {
            aCase.setSubjectId(resource.toString());

            List<String> csIds = jenaLibrary.getStringValueBySP(model, resource, "common:type.object.id");
            if (csIds.size() > 0)
                aCase.setCaseId(csIds.get(0));

            List<String> csNames = jenaLibrary.getStringValueBySP(model, resource, "common:type.object.name");
            if (csNames.size() > 0)
                aCase.setCaseName(csNames.get(0));

            List<String> csTypes = jenaLibrary.getStringValueBySP(model, resource, "gongan:gongan.case.category");
            if (csTypes.size() > 0)
                aCase.setCaseType(String.join(",", csTypes));

        }

        val biluIter1 = jenaLibrary.getStatementsBySP(model, resource, "gongan:gongan.case.bilu");
        while (biluIter1.hasNext()){
            Resource biluResource = biluIter1.next().getResource();
            if(!biluCache.containsKey(biluResource)){
                Bilu bilu = getBiluInfo(model, biluResource);
                biluCache.put(biluResource.toString(), bilu);
            }
            Bilu bilu = biluCache.get(biluResource.toString());
            aCase.getBilus().add(bilu);

            for (String pSubjectId : bilu.getConnections().keySet())
            {
                if(!aCase.getConnections().containsKey(pSubjectId))
                    aCase.getConnections().put(pSubjectId, bilu.getConnections().get(pSubjectId));
                else
                    aCase.getConnections().put(pSubjectId, bilu.getConnections().get(pSubjectId) + "；" + aCase.getConnections().get(pSubjectId));
            }
        }
        return aCase;
    }

    public List<CaseBaseInfo> getAllCaseBaseInfo() {
        if (allSimpleCases.size() > 0)
            return allSimpleCases.values().stream().collect(Collectors.toList());
        else {
            try {
                jenaLibrary.openReadTransaction();
                Model model = jenaLibrary.getRuntimeModel();

                val iterator = jenaLibrary.getStatementsByEntityType(model, "gongan:gongan.case");

                while (iterator.hasNext()) {
                    CaseBaseInfo aCase = getCaseBaseInfo(model, iterator.next().getSubject());
                    allSimpleCases.put(aCase.getSubjectId(), aCase);
                }

                return allSimpleCases.values().stream().collect(Collectors.toList());

            } finally {
                jenaLibrary.closeTransaction();
            }
        }
    }

    private CaseBaseInfo getCaseBaseInfo(Model model, Resource resource) {
        CaseBaseInfo caseBaseInfo = new CaseBaseInfo();

        caseBaseInfo.setSubjectId(resource.toString());

        List<String> ids = jenaLibrary.getStringValueBySP(model, resource, "common:type.object.id");
        if (ids.size() > 0)
            caseBaseInfo.setCaseId(ids.get(0));

        List<String> names = jenaLibrary.getStringValueBySP(model, resource, "common:type.object.name");
        if (names.size() > 0)
            caseBaseInfo.setCaseName(names.get(0));

        List<String> types = jenaLibrary.getStringValueBySP(model, resource, "gongan:gongan.case.category");
        if (types.size() > 0)
            caseBaseInfo.setCaseType(String.join(",", types));

        // count of bilu
        val biluIter1 = jenaLibrary.getStatementsBySP(model, resource, "gongan:gongan.case.bilu");
        if (biluIter1.hasNext())
            caseBaseInfo.setBiluNumber(Iterators.size(biluIter1));

        // set suspect
        caseBaseInfo.setSuspects(new LinkedList<>());

        val bilus = Lists.newArrayList(jenaLibrary.getStatementsBySP(model, resource, "gongan:gongan.case.bilu")).stream().map(s -> s.getResource().toString()).distinct().collect(Collectors.toList());
        List<String> biluConnections = Lists.newArrayList(jenaLibrary.getStatementsByBatchPO(model, "common:common.connection.from", bilus)).stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());

        for (String connection : biluConnections) {
            List<String> connectTypes = jenaLibrary.getStringValueBySP(model, model.getResource(connection), "common:common.connection.type");
            if (connectTypes.contains("common:common.connection.BiluEntityXianyiren")) {
                val toStatements = Lists.newArrayList(jenaLibrary.getStatementsBySP(model, model.getResource(connection), "common:common.connection.to"))
                        .stream().map(s -> s.getResource().toString()).distinct().collect(Collectors.toList());
                caseBaseInfo.getSuspects().addAll(jenaLibrary.getStringValuesByBatchSP(model, toStatements,"common:type.object.name"));
            }
        }

        return caseBaseInfo;
    }
}