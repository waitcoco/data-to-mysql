package com.mycompany.tahiti.analysis.repository;

import com.google.common.collect.Lists;
import com.mycompany.tahiti.analysis.configuration.Configs;
import com.mycompany.tahiti.analysis.jena.TdbJenaLibrary;
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
    TdbJenaLibrary jenaLibrary;

    private Map<String, Case> allCases = new HashMap<>();
    private Map<String, Bilu> allBilus = new HashMap<>();
    private Map<String, Person> allPersons = new HashMap<>();

    // caseId, Case
    public Map<String, Case> getCases() {
        return allCases;
    }

    // biluId, Bilu
    public Map<String, Bilu> getBilus() { return allBilus; }

    // subjectId, Person
    public Map<String, Person> getPersons() { return allPersons;}

    public boolean updateCases() {
        try {
            allCases.clear();
            allBilus.clear();
            allPersons.clear();

            jenaLibrary.openReadTransaction();
            Model model = jenaLibrary.getModel(Configs.getConfig("jenaModelName"));
            val iterator = jenaLibrary.getStatementsByEntityType(model, "gongan:gongan.case");

            while (iterator.hasNext()) {
                Statement statement = iterator.next();
                Resource resource = statement.getSubject();
                getCaseInfo(model, resource);
            }
        } finally {
            jenaLibrary.closeTransaction();
        }

        return true;
    }

    public void getCaseInfo(Model model, Resource resource) {
        Case aCase = new Case();
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

        // count of bilu
        val biluIter1 = jenaLibrary.getStatementsBySP(model, resource, "gongan:gongan.case.bilu");
        while (biluIter1.hasNext()) {
            Bilu bilu = new Bilu();
            Statement statement = biluIter1.next();
            Resource biluResource = statement.getResource();

            bilu.setSubjectId(biluResource.toString());

            //bilu id
            List<String> bilu_id_list = jenaLibrary.getStringValueBySP(model, biluResource, "common:type.object.id");
            bilu.setBiluId(bilu_id_list.size() > 0 ? bilu_id_list.get(0) : "");

            //bilu name
            List<String> bilu_name_list = jenaLibrary.getStringValueBySP(model, biluResource, "common:type.object.name");
            bilu.setName(bilu_name_list.size() > 0 ? bilu_name_list.get(0) : "");

            //bilu content
            List<String> bilu_content_list = jenaLibrary.getStringValueBySP(model, biluResource, "common:common.document.contentStream");
            bilu.setContent(bilu_content_list.size() > 0 ? bilu_content_list.get(0) : "");

            // set persons
            val entities = Lists.newArrayList(jenaLibrary.getStatementsBySP(model, biluResource, "gongan:gongan.bilu.entity")).stream().map(s -> s.getResource().toString()).distinct().collect(Collectors.toList());
            val persons = Lists.newArrayList(jenaLibrary.getStatementsByPOValue(model, "common:type.object.type", "common:person.person")).stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());
            persons.retainAll(entities);

            List<String> biluConnections = Lists.newArrayList(jenaLibrary.getStatementsByPO(model, "common:common.connection.from", biluResource)).stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());
            for (String personSubject : persons) {
                if(allPersons.containsKey(personSubject)) {

                    Person person = allPersons.get(personSubject);
                    person.getCaseList().add(aCase.getCaseId());
                    person.getBiluList().add(bilu.getBiluId());

                    bilu.getPersons().add(person);
                }
                else {
                    Person person = new Person();

                    person.setSubjectId(personSubject);
                    val pNames = jenaLibrary.getStringValueBySP(model, model.getResource(personSubject), "common:type.object.name");
                    if (pNames.size() > 0)
                        person.setName(pNames.get(0));

                    val personIdentities = jenaLibrary.getStatementsBySP(model, model.getResource(personSubject), "common:person.person.identification");
                    if (personIdentities.hasNext()) {
                        val personIds = jenaLibrary.getStringValueBySP(model, personIdentities.next().getResource(), "common:person.identification.number");
                        if (personIds.size() > 0)
                            person.setIdentity(personIds.get(0));
                    }

                    val contactIters = jenaLibrary.getStatementsBySP(model, model.getResource(personSubject), "common:person.person.contact");
                    if (contactIters.hasNext()) {
                        val contacts = jenaLibrary.getStringValueBySP(model, contactIters.next().getResource(), "common:person.contact.number");
                        if (contacts.size() > 0)
                            person.setPhone(contacts.get(0));
                    }

                    val birthdays = jenaLibrary.getStringValueBySP(model, model.getResource(personSubject), "common:person.person.birthDate");
                    if (birthdays.size() > 0)
                        person.setBirthDay(birthdays.get(0));

                    val genders = jenaLibrary.getStringValueBySP(model, model.getResource(personSubject), "common:person.person.gender");
                    if (genders.size() > 0) {
                        if (genders.get(0).toLowerCase().equals("female"))
                            person.setGender("女");
                        else if (genders.get(0).toLowerCase().equals("male"))
                            person.setGender("男");
                    }

                    // set connection
                    List<String> connectionVal = Lists.newArrayList(jenaLibrary.getStatementsByPO(model, "common:common.connection.to", model.getResource(personSubject))).stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());
                    connectionVal.retainAll(biluConnections);

                    String role = "";
                    for (String connection : connectionVal) {
                        val connectionType = jenaLibrary.getStringValueBySP(model, model.getResource(connection), "common:common.connection.type");

                        if (connectionType.contains("common:common.connection.BiluEntityXianyiren"))
                            role += "嫌疑人；";
                        if (connectionType.contains("common:common.connection.BiluEntityZhengren"))
                            role += "证人；";
                        if (connectionType.contains("common:common.connection.BiluEntityBaoanren"))
                            role += "报案人；";
                        if (connectionType.contains("common:common.connection.BiluEntityDangshiren"))
                            role += "当事人；";
                        if (connectionType.contains("common:common.connection.BiluEntityShouhairen"))
                            role += "受害人；";
                    }

                    int roleLength = role.length();
                    if (roleLength > 0)
                        bilu.getConnections().put(personSubject, role.substring(0, roleLength - 1));

                    person.getCaseList().add(aCase.getCaseId());
                    person.getBiluList().add(bilu.getBiluId());

                    allPersons.put(personSubject, person);

                    bilu.getPersons().add(person);
                }
            }

            // get all things
            val things = Lists.newArrayList(jenaLibrary.getStatementsBySP(model, biluResource, "gongan:gongan.bilu.thing")).stream().map(s -> s.getResource().toString()).distinct().collect(Collectors.toList());

            // set phone
            val phones = Lists.newArrayList(jenaLibrary.getStatementsByPOValue(model, "common:type.object.type", "common:thing.phone")).stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());
            phones.retainAll(things);
            for(String phone : phones) {
                List<String> phoneNums = jenaLibrary.getStringValueBySP(model, model.getResource(phone), "common:thing.phone.phoneNumber");
                if(phoneNums.size() > 0)
                    bilu.getPhones().put(phone, phoneNums.get(0));
            }
            // set bank cards
            val bankCards = Lists.newArrayList(jenaLibrary.getStatementsByPOValue(model, "common:type.object.type", "common:thing.bankcard")).stream().map(s -> s.getSubject().toString()).distinct().collect(Collectors.toList());
            bankCards.retainAll(things);
            for(String bankCard : bankCards) {
                List<String> bankCardNums = jenaLibrary.getStringValueBySP(model, model.getResource(bankCard), "common:thing.bankcard.bankCardId");
                if(bankCardNums.size() > 0)
                    bilu.getBankCards().put(bankCard, bankCardNums.get(0));
            }

            allBilus.put(bilu.getBiluId(), bilu);
            aCase.getBilus().add(bilu);
        }

        allCases.put(aCase.getCaseId(), aCase);
    }
}
