package com.joao.desafio.controller;

import com.joao.desafio.model.Agenda;
import com.joao.desafio.model.Vote;
import com.joao.desafio.model.VotingSession;
import com.joao.desafio.service.VotingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/agendas")
public class VotingController {

    private final VotingService service;

    public VotingController(VotingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Agenda> createAgenda(@RequestBody String description) {
        Agenda agenda = service.createAgenda(description);
        return ResponseEntity.ok(agenda);
    }

    @PostMapping("/{agendaId}/session")
    public ResponseEntity<VotingSession> openSession(
            @PathVariable Long agendaId,
            @RequestParam(required = false) Integer duration
    ) {
        VotingSession session = service.openSession(agendaId, duration);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{agendaId}/vote")
    public ResponseEntity<Vote> vote(
            @PathVariable Long agendaId,
            @PathVariable Long associateId,
            @RequestParam Boolean vote,
            @RequestParam String cpf
    ) {
        Vote newVote = service.vote(agendaId, associateId, vote, cpf);
        return ResponseEntity.ok(newVote);
    }

    @GetMapping("/{agendaId}/result")
    public ResponseEntity<Map<String, Long>> votingResult(@PathVariable Long agendaId) {
        Map<String, Long> result = service.votingResult(agendaId);
        return ResponseEntity.ok(result);
    }
}
