package com.example.authapp.service;

import com.example.authapp.dto.*;
import com.example.authapp.entity.*;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.WorkflowDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** All workflow-builder mutations and activation invariants live here. */
@Service
public class WorkflowAdminService {
    private final WorkflowDefinitionRepository definitionRepository;

    public WorkflowAdminService(WorkflowDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponseDTO> list(boolean includeInactive) {
        return definitionRepository.findAll().stream()
                .filter(d -> includeInactive || d.isActive())
                .sorted(Comparator.comparing(WorkflowDefinition::getName, String.CASE_INSENSITIVE_ORDER))
                .map(WorkflowDefinitionResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponseDTO> listByActive(boolean active) {
        return definitionRepository.findAll().stream()
                .filter(d -> d.isActive() == active)
                .sorted(Comparator.comparing(WorkflowDefinition::getName, String.CASE_INSENSITIVE_ORDER))
                .map(WorkflowDefinitionResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowDefinitionResponseDTO get(Long id) {
        return WorkflowDefinitionResponseDTO.from(find(id));
    }

    @Transactional
    public WorkflowDefinitionResponseDTO create(WorkflowDefinitionRequestDTO dto) {
        String type = normalizeType(dto.getWorkflowType());
        ensureTypeAvailable(type, null);
        if (dto.getName() == null || dto.getName().isBlank()) throw new WorkflowException("Workflow name is required.");
        WorkflowDefinition definition = new WorkflowDefinition(
                clean(dto.getName()), dto.getDescription(), type);
        definition.setActive(false);
        if (dto.getSteps() != null) {
            replaceSteps(definition, dto.getSteps());
        }
        definition = definitionRepository.save(definition);
        if (Boolean.TRUE.equals(dto.getActive())) {
            validateForActivation(definition);
            definition.setActive(true);
            definition = definitionRepository.save(definition);
        }
        return WorkflowDefinitionResponseDTO.from(definition);
    }

    @Transactional
    public WorkflowDefinitionResponseDTO update(Long id, WorkflowDefinitionRequestDTO dto) {
        WorkflowDefinition definition = find(id);
        String type = normalizeType(dto.getWorkflowType());
        ensureTypeAvailable(type, id);
        if (dto.getName() == null || dto.getName().isBlank()) throw new WorkflowException("Workflow name is required.");
        definition.setName(clean(dto.getName()));
        definition.setDescription(dto.getDescription());
        definition.setWorkflowType(type);
        if (dto.getSteps() != null) {
            replaceSteps(definition, dto.getSteps());
        }
        if (dto.getActive() != null) {
            if (dto.getActive()) validateForActivation(definition);
            definition.setActive(dto.getActive());
        }
        return WorkflowDefinitionResponseDTO.from(definitionRepository.save(definition));
    }

    /** DELETE is intentionally a soft delete: historical instances retain their definition. */
    @Transactional
    public void deactivate(Long id) {
        WorkflowDefinition definition = find(id);
        definition.setActive(false);
        definitionRepository.save(definition);
    }

    @Transactional
    public WorkflowDefinitionResponseDTO deactivateAndGet(Long id) {
        WorkflowDefinition definition = find(id);
        definition.setActive(false);
        return WorkflowDefinitionResponseDTO.from(definitionRepository.save(definition));
    }

    @Transactional
    public WorkflowDefinitionResponseDTO activate(Long id) {
        WorkflowDefinition definition = find(id);
        validateForActivation(definition);
        definition.setActive(true);
        return WorkflowDefinitionResponseDTO.from(definitionRepository.save(definition));
    }

    @Transactional
    public WorkflowStepResponseDTO addStep(Long workflowId, WorkflowStepRequestDTO dto) {
        WorkflowDefinition definition = find(workflowId);
        WorkflowStep step = toStep(definition, dto, nextOrder(definition));
        definition.getSteps().add(step);
        validateOrdering(definition);
        if (definition.isActive()) validateForActivation(definition);
        definitionRepository.save(definition);
        return WorkflowStepResponseDTO.from(step);
    }

    @Transactional
    public WorkflowStepResponseDTO updateStep(Long workflowId, Long stepId, WorkflowStepRequestDTO dto) {
        WorkflowDefinition definition = find(workflowId);
        WorkflowStep step = findStep(definition, stepId);
        step.setName(clean(dto.getName()));
        step.setStepType(parseStepType(dto.getStepType()));
        step.setAssignedRole(parseRole(dto.getAssignedRole()));
        step.setRequired(dto.getRequired() == null || dto.getRequired());
        step.setConfiguration(dto.getConfiguration());
        if (dto.getStepOrder() != null) step.setStepOrder(dto.getStepOrder());
        validateStepFields(step);
        validateOrdering(definition);
        if (definition.isActive()) validateForActivation(definition);
        definitionRepository.save(definition);
        return WorkflowStepResponseDTO.from(step);
    }

    @Transactional
    public void deleteStep(Long workflowId, Long stepId) {
        WorkflowDefinition definition = find(workflowId);
        WorkflowStep step = findStep(definition, stepId);
        definition.getSteps().remove(step);
        // Keep drafts usable after deletion and avoid leaving a gap in order.
        renumber(definition);
        if (definition.isActive()) validateForActivation(definition);
        definitionRepository.save(definition);
    }

    @Transactional
    public WorkflowDefinitionResponseDTO reorder(Long workflowId, WorkflowReorderDTO dto) {
        WorkflowDefinition definition = find(workflowId);
        List<WorkflowStep> steps = definition.getSteps();
        if (steps.stream().anyMatch(step -> step.getId() == null)) {
            throw new WorkflowException("Workflow steps must be saved before they can be reordered.");
        }
        if (dto.getStepIds().size() != steps.size()
                || new HashSet<>(dto.getStepIds()).size() != steps.size()) {
            throw new WorkflowException("Reorder must include every workflow step exactly once.");
        }
        Map<Long, WorkflowStep> byId = steps.stream().collect(Collectors.toMap(WorkflowStep::getId, Function.identity()));
        for (int i = 0; i < dto.getStepIds().size(); i++) {
            WorkflowStep step = byId.get(dto.getStepIds().get(i));
            if (step == null) throw new WorkflowException("Step " + dto.getStepIds().get(i) + " does not belong to this workflow.");
            step.setStepOrder(i + 1);
        }
        validateOrdering(definition);
        if (definition.isActive()) validateForActivation(definition);
        return WorkflowDefinitionResponseDTO.from(definitionRepository.save(definition));
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponseDTO> activeCatalog() {
        return list(false);
    }

    private WorkflowDefinition find(Long id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Workflow " + id + " not found."));
    }

    private WorkflowStep findStep(WorkflowDefinition definition, Long id) {
        return definition.getSteps().stream().filter(s -> Objects.equals(s.getId(), id)).findFirst()
                .orElseThrow(() -> new WorkflowException("Step " + id + " does not belong to workflow " + definition.getId() + "."));
    }

    private void replaceSteps(WorkflowDefinition definition, List<WorkflowStepRequestDTO> requests) {
        definition.getSteps().clear();
        int order = 1;
        for (WorkflowStepRequestDTO dto : requests) {
            int requestedOrder = dto.getStepOrder() == null ? order : dto.getStepOrder();
            definition.getSteps().add(toStep(definition, dto, requestedOrder));
            order++;
        }
        validateOrdering(definition);
    }

    private WorkflowStep toStep(WorkflowDefinition definition, WorkflowStepRequestDTO dto, int defaultOrder) {
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowDefinition(definition);
        step.setStepOrder(dto.getStepOrder() == null ? defaultOrder : dto.getStepOrder());
        step.setName(clean(dto.getName()));
        step.setStepType(parseStepType(dto.getStepType()));
        step.setAssignedRole(parseRole(dto.getAssignedRole()));
        step.setRequired(dto.getRequired() == null || dto.getRequired());
        step.setConfiguration(dto.getConfiguration());
        validateStepFields(step);
        return step;
    }

    private void validateForActivation(WorkflowDefinition definition) {
        if (definition.getSteps().isEmpty()) throw new WorkflowException("A workflow must have at least one step before activation.");
        validateOrdering(definition);
        definition.getSteps().forEach(this::validateStepFields);
    }

    private void validateOrdering(WorkflowDefinition definition) {
        List<Integer> orders = definition.getSteps().stream().map(WorkflowStep::getStepOrder).sorted().toList();
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i) != i + 1) {
                throw new WorkflowException("Workflow steps must be ordered consecutively starting at 1.");
            }
        }
    }

    private void validateStepFields(WorkflowStep step) {
        if (step.getStepOrder() < 1) throw new WorkflowException("Step order must be at least 1.");
        if (step.getName() == null || step.getName().isBlank()) throw new WorkflowException("Step name is required.");
        if (step.getStepType() == null) throw new WorkflowException("Step type is required.");
        if (step.getAssignedRole() == null) throw new WorkflowException("Each step must have an assigned role.");
    }

    private int nextOrder(WorkflowDefinition definition) {
        return definition.getSteps().stream().mapToInt(WorkflowStep::getStepOrder).max().orElse(0) + 1;
    }

    private void renumber(WorkflowDefinition definition) {
        List<WorkflowStep> ordered = definition.getOrderedSteps();
        for (int i = 0; i < ordered.size(); i++) ordered.get(i).setStepOrder(i + 1);
    }

    private void ensureTypeAvailable(String type, Long currentId) {
        definitionRepository.findAll().stream()
                .filter(d -> d.getWorkflowType() != null && d.getWorkflowType().equalsIgnoreCase(type))
                .filter(d -> !Objects.equals(d.getId(), currentId))
                .findFirst()
                .ifPresent(d -> { throw new WorkflowException("Workflow type '" + type + "' is already in use."); });
    }

    private String normalizeType(String type) {
        String value = clean(type);
        if (value == null || value.isBlank()) throw new WorkflowException("Workflow type is required.");
        value = value.toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z][A-Z0-9_]*")) throw new WorkflowException("Workflow type must contain only letters, numbers, and underscores.");
        return value;
    }

    private String clean(String value) { return value == null ? null : value.trim(); }

    private StepType parseStepType(String value) {
        try { return StepType.valueOf(clean(value).toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new WorkflowException("Unknown step type '" + value + "'."); }
    }

    private ERole parseRole(String value) {
        try {
            String normalized = clean(value).toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            if (!normalized.startsWith("ROLE_")) normalized = "ROLE_" + normalized;
            return ERole.valueOf(normalized);
        } catch (Exception ex) { throw new WorkflowException("Unknown assigned role '" + value + "'."); }
    }
}
