package iuh.week05_lab_huynhhoangphuc_21036541.backend.repositories;


import iuh.week05_lab_huynhhoangphuc_21036541.backend.models.CandidateSkill;
import iuh.week05_lab_huynhhoangphuc_21036541.backend.models.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, Skill> {
}
