package com.joao.desafio.service;

import com.joao.desafio.config.RabbitMQConfig;
import com.joao.desafio.model.Agenda;
import com.joao.desafio.model.Vote;
import com.joao.desafio.model.VotingSession;
import com.joao.desafio.repository.AgendaRepository;
import com.joao.desafio.repository.VoteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class VotingService {

    private static final String CPF_VALIDATION_URL = "https://user-info.herokuapp.com/users/";

    private final AgendaRepository agendaRepo;

    private final VoteRepository voteRepo;

    private final RestTemplate restTemplate;

    private final RabbitTemplate rabbitTemplate;

    public VotingService(AgendaRepository agendaRepo,
                         VoteRepository voteRepo, RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        this.agendaRepo = agendaRepo;
        this.voteRepo = voteRepo;
        this.restTemplate = restTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Agenda createAgenda(String description) {

        Agenda agenda = new Agenda();
        agenda.setDescription(description);

        return agendaRepo.save(agenda);
    }

    public VotingSession openSession(Long agendaId, Integer durationInMinutes) {

        Agenda agenda = agendaRepo.findById(agendaId)
                .orElseThrow(() -> new EntityNotFoundException("Pauta não encontrada"));

        VotingSession session = new VotingSession();
        session.setStart(LocalDateTime.now());
        session.setEnd(LocalDateTime.now().plusMinutes(durationInMinutes != null ? durationInMinutes : 1));

        agenda.setVotingSession(session);
        agendaRepo.save(agenda);

        return session;
    }

    public Vote vote(Long agendaId, Long associateId, Boolean vote, String cpf) {

        validateAssociateCanVote(cpf);

        Agenda agenda = agendaRepo.findById(agendaId)
                .orElseThrow(() -> new EntityNotFoundException("Pauta não encontrada."));

        if (LocalDateTime.now().isAfter(agenda.getVotingSession().getEnd())) {
            throw new IllegalStateException("A sessão de votação está encerrada");
        }

        voteRepo.findByAgendaIdAndAssociateId(agendaId, associateId).ifPresent(v -> {
            throw new IllegalStateException("Associado já votou nesta pauta.");
        });

        Vote newVote = new Vote();
        newVote.setAssociateId(associateId);
        newVote.setVote(vote);
        newVote.setAgenda(agenda);

        return voteRepo.save(newVote);
    }

    public void closeVotingSession(Long agendaId) {
        Agenda agenda = agendaRepo.findById(agendaId)
                .orElseThrow(() -> new EntityNotFoundException("Pauta não encontrada."));

        Map<String, Long> result = votingResult(agenda.getId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NAME, result);
    }

    public Map<String, Long> votingResult(Long agendaId) {
        Agenda agenda = agendaRepo.findById(agendaId)
                .orElseThrow(() -> new EntityNotFoundException("Pauta não encontrada."));

        long yesVote = agenda.getVotes().stream().filter(Vote::getVote).count();
        long noVote = agenda.getVotes().size() - yesVote;

        Map<String, Long> result = new HashMap<>();
        result.put("Sim", yesVote);
        result.put("Não", noVote);

        return result;
    }

    private void validateAssociateCanVote(String cpf) {
        String url = CPF_VALIDATION_URL + cpf;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (!"ABLE_TO_VOTE".equals(Objects.requireNonNull(response.getBody()).get("status"))) {
                throw new IllegalStateException("Associado não está apto para votar.");
            }
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalStateException("CPF inválido.");
        }
    }
}
