package com.joao.desafio.repository;

import com.joao.desafio.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByAgendaIdAndAssociateId(Long agendaId, Long associateId);

}
