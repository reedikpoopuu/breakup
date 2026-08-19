package com.example.demo.eval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractEvalSampleRepository extends JpaRepository<ContractEvalSample, Long> {

    List<ContractEvalSample> findAllByOrderByCreatedAtDesc();
}
