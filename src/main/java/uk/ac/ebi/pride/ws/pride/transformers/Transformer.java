package uk.ac.ebi.pride.ws.pride.transformers;

import uk.ac.ebi.pride.archive.dataprovider.common.Tuple;
import uk.ac.ebi.pride.archive.dataprovider.data.ptm.IdentifiedModification;
import uk.ac.ebi.pride.archive.dataprovider.data.ptm.IdentifiedModificationProvider;
import uk.ac.ebi.pride.archive.dataprovider.param.CvParam;
import uk.ac.ebi.pride.archive.dataprovider.param.CvParamProvider;
import uk.ac.ebi.pride.archive.dataprovider.reference.Reference;
import uk.ac.ebi.pride.archive.dataprovider.user.Contact;
import uk.ac.ebi.pride.archive.dataprovider.user.ContactProvider;
import uk.ac.ebi.pride.archive.repo.repos.project.Project;
import uk.ac.ebi.pride.archive.repo.repos.project.ProjectCvParam;
import uk.ac.ebi.pride.archive.repo.repos.project.ProjectTag;
import uk.ac.ebi.pride.archive.repo.repos.user.User;
import uk.ac.ebi.pride.ws.pride.models.dataset.PrideProject;
import uk.ac.ebi.pride.ws.pride.utils.WsContastants;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This code is licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * ==Overview==
 *
 * @author ypriverol on 29/10/2018.
 */
public class Transformer {


    /**
     * Transform a list of projects from Oracle
     * @param oracleProjects list of oracle projects
     * @return list of projects from API
     */
    public static List<PrideProject> transformPrivateProjects(List<Project> oracleProjects){
        return oracleProjects.stream().map(Transformer::transformOracleProject).collect(Collectors.toList());
    }

    /**
     * Transform a project
     * @param oracleProject oracle Project
     * @return API project
     */
    public static PrideProject transformOracleProject(Project oracleProject) {
        String doi = null;
        if (oracleProject.getDoi() != null && oracleProject.getDoi().isPresent())
            doi = oracleProject.getDoi().get();

        Collection<CvParamProvider> instruments = new ArrayList<>();
        if(oracleProject.getInstruments() != null && oracleProject.getInstruments().size() > 0)
            instruments = oracleProject.getInstruments().stream().map( x-> new CvParam(x.getCvLabel(), x.getAccession(),x.getName(), x.getValue())).collect(Collectors.toList());

        Collection<CvParamProvider> softwares = new ArrayList<>();
        if(oracleProject.getSoftware() != null && oracleProject.getSoftware().size() > 0)
            softwares = oracleProject.getSoftware().stream().map( x -> new CvParam(x.getCvLabel(), x.getAccession(),x.getName(),x.getValue())).collect(Collectors.toList());


        Collection<ContactProvider> labPIs = new ArrayList<>();
        if(oracleProject.getLabHeads() != null && oracleProject.getLabHeads().size() > 0)
            labPIs = oracleProject.getLabHeads().stream()
                    .map( x-> new Contact(x.getTitle(), x.getFirstName(),x.getLastName(),
                            String.valueOf(x.getId()),x.getAffiliation(), x.getEmail(), x.getCountry(),x.getOrcid()))
                    .collect(Collectors.toList());

        Collection<ContactProvider> submitters = new ArrayList<>();
        if(oracleProject.getSubmitter() != null){
            User x = oracleProject.getSubmitter();
            submitters.add(new Contact(x.getTitle(), x.getFirstName(),x.getLastName(),
                    String.valueOf(x.getId()),x.getAffiliation(), x.getEmail(), x.getCountry(),x.getOrcid()));
        }

        Collection<CvParamProvider> quantificationMethods = new ArrayList<>();
        if(oracleProject.getQuantificationMethods() != null && oracleProject.getQuantificationMethods().size() > 0)
            quantificationMethods = oracleProject.getQuantificationMethods().stream()
                    .map(x -> new CvParam(x.getCvLabel(), x.getAccession(), x.getName(), x.getValue()))
                    .collect(Collectors.toList());

        //References
        List<Reference> references = oracleProject.getReferences().stream()
                .map( reference -> new Reference(reference.getReferenceLine(), reference.getPubmedId(), reference.getDoi()))
                .collect(Collectors.toList());

        //Modifications
        Set<CvParamProvider> ptms = oracleProject.getPtms().stream()
                .map(ptm -> new CvParam(ptm.getCvLabel(), ptm.getAccession(), ptm.getName(), ptm.getValue()))
                .collect(Collectors.toSet());


        //Get software information
        Set<CvParam> softwareList = oracleProject.getSoftware()
                .stream()
                .filter(software -> software.getCvParam() != null )
                .map(software -> new CvParam(software.getCvParam().getCvLabel(), software.getCvParam().getAccession(),
                        software.getCvParam().getName(), software.getCvParam().getValue()))
                .collect(Collectors.toSet());

        // Project Tags
        List<String> projectTags = oracleProject.getProjectTags().stream()
                .map(ProjectTag::getTag)
                .map(Transformer::convertSentenceStyle)
                .collect(Collectors.toList());

        // Project Keywords
        List<String> keywords = oracleProject.getKeywords().stream()
                .map(Transformer::convertSentenceStyle).collect(Collectors.toList());

        Collection<CvParamProvider> diseases = new ArrayList<>();
        Collection<CvParamProvider> organisms = new ArrayList<>();
        Collection<CvParamProvider> organismParts = new ArrayList<>();


        if(oracleProject.getSamples() != null && oracleProject.getSamples().size() > 0){
            for( ProjectCvParam param: oracleProject.getSamples()){
                if(param.getCvParam() != null){

                    CvParam value = new CvParam(param.getCvLabel(),param.getAccession(), param.getName(),param.getValue());
                    if(param.getCvLabel().equalsIgnoreCase(WsContastants.CV_LABEL_ORGANISM)){
                        organisms.add(value);
                    }else if(param.getCvLabel().equalsIgnoreCase(WsContastants.CV_LABEL_CELL_COMPONENT) || param.getCvLabel().equalsIgnoreCase(WsContastants.CV_LABEL_CELL_TISSUE)){
                        ((ArrayList<CvParamProvider>) organismParts).add(value);
                    }else if(param.getCvLabel().equalsIgnoreCase(WsContastants.CV_LABEL_DISEASE)){
                        ((ArrayList<CvParamProvider>) diseases).add(value);
                    }
                }
            }
        }

        return PrideProject.builder()
                .accession(oracleProject.getAccession())
                .dataProcessingProtocol(oracleProject.getDataProcessingProtocol())
                .sampleProcessingProtocol(oracleProject.getSampleProcessingProtocol())
                .projectDescription(oracleProject.getProjectDescription())
                .doi(doi)
                .instruments(instruments)
                .softwares(softwares)
                .labPIs(labPIs)
                .submitters(submitters)
                .submissionDate(oracleProject.getSubmissionDate())
                .quantificationMethods(quantificationMethods)
                .identifiedPTMStrings(ptms)
                .projectTags(projectTags)
                .keywords(keywords)
                .diseases(diseases)
                .organisms(organisms)
                .organismParts(organismParts)
                .build();
    }

    /**
     * Get convert sentence to Capitalize Style
     * @param sentence original sentence
     * @return Capitalize sentence
     */
    private static String convertSentenceStyle(String sentence){
        sentence = sentence.toLowerCase().trim();
        return org.apache.commons.lang3.StringUtils.capitalize(sentence);
    }


    /**
     * Transform a List of {@link IdentifiedModificationProvider} to a List of {@link IdentifiedModification}
     * @param oldPtms List of {@link IdentifiedModificationProvider}
     * @return List of {@link IdentifiedModification}
     */
    public static List<IdentifiedModification> transformModifications(Collection<? extends IdentifiedModificationProvider> oldPtms){
        return oldPtms.stream().map(ptm -> {
            CvParam ptmName = new CvParam(ptm.getModificationCvTerm().getCvLabel(),
                    ptm.getModificationCvTerm().getAccession(),
                    ptm.getModificationCvTerm().getName(),
                    ptm.getModificationCvTerm().getValue());

            CvParam neutral = null;
            if(ptm.getNeutralLoss() != null)
                neutral = new CvParam(ptm.getNeutralLoss().getCvLabel(),
                        ptm.getNeutralLoss().getAccession(),
                        ptm.getNeutralLoss().getName(),
                        ptm.getNeutralLoss().getValue());

            List<Tuple<Integer, Set<? extends CvParamProvider>>> ptmPositions = ptm.getPositionMap().stream().map(position ->{
                Collection<CvParamProvider> scores = (Collection<CvParamProvider>) position.getValue();
                Integer currentPosition = position.getKey();
                Set<CvParam> newScores = scores.stream().map(score -> new CvParam(score.getCvLabel(),
                        score.getAccession(), score.getName(),
                        score.getValue())).collect(Collectors.toSet());
                return new Tuple<Integer, Set<? extends CvParamProvider>>(currentPosition, newScores);
            }).collect(Collectors.toList());

            IdentifiedModification newPTM = new IdentifiedModification(neutral, ptmPositions, ptmName, new HashSet<>());

            return newPTM;
        }).collect(Collectors.toList());
    }

}
