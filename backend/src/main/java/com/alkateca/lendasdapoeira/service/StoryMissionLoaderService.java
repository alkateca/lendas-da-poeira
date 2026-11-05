package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.story.StoryMissionDefinition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StoryMissionLoaderService {

    private final Map<String, StoryMissionDefinition> missionsById = new ConcurrentHashMap<>();
    private final ObjectMapper yamlMapper;

    public StoryMissionLoaderService() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PostConstruct
    public void loadAllMissions() {
        missionsById.clear();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:missions/**/*.yaml");

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    StoryMissionDefinition mission = yamlMapper.readValue(is, StoryMissionDefinition.class);
                    if (mission != null && mission.id() != null) {
                        missionsById.put(mission.id(), mission);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar arquivos YAML do Modo História", e);
        }
    }

    public List<StoryMissionDefinition> listAllMissions() {
        return missionsById.values().stream()
                .sorted(Comparator.comparing(StoryMissionDefinition::cor)
                        .thenComparingInt(StoryMissionDefinition::capitulo))
                .toList();
    }

    public Optional<StoryMissionDefinition> getMissionById(String missionId) {
        return Optional.ofNullable(missionsById.get(missionId));
    }
}
