package iuh.week05_lab_huynhhoangphuc_21036541.frontend.controllers;

import com.neovisionaries.i18n.CountryCode;
import iuh.week05_lab_huynhhoangphuc_21036541.backend.models.*;
import iuh.week05_lab_huynhhoangphuc_21036541.backend.repositories.AddressRepository;
import iuh.week05_lab_huynhhoangphuc_21036541.backend.repositories.CandidateRepository;
import iuh.week05_lab_huynhhoangphuc_21036541.backend.repositories.SkillRepository;
import iuh.week05_lab_huynhhoangphuc_21036541.backend.services.CandidateServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/candidates")
public class CandidateController {
    @Autowired
    private CandidateRepository candidateRepository;
    @Autowired
    private CandidateServices candidateServices;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private SkillRepository skillRepository;



    @GetMapping("/list")
    public String showCandidateList(Model model) {
        model.addAttribute("candidates", candidateRepository.findAll());
        return "candidates/list_no_paging";
    }

    @GetMapping("")
    public String showCandidateListPaging(Model model,
                                          @RequestParam("page") Optional<Integer> page,
                                          @RequestParam("size") Optional<Integer> size) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(10);
        /*Page<Candidate> candidatePage= candidateServices.findPaginated(
                PageRequest.of(currentPage - 1, pageSize)
        );*/
        Page<Candidate> candidatePage = candidateServices.findAll(currentPage - 1,
                pageSize, "id", "asc");

        model.addAttribute("candidatePage", candidatePage);

        int totalPages = candidatePage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        return "candidates/list";
    }

    @GetMapping("/show-add-form")
    public ModelAndView add(Model model) {
        ModelAndView modelAndView = new ModelAndView();
        Candidate candidate = new Candidate();
        candidate.setAddress(new Address());
        modelAndView.addObject("candidate", candidate);
        modelAndView.addObject("address", candidate.getAddress());
        modelAndView.addObject("countries", CountryCode.values());

        List<Skill> allSkills = skillRepository.findAll();
        modelAndView.addObject("allSkills", allSkills);

        modelAndView.setViewName("candidates/add");
        return modelAndView;
    }

    @PostMapping("/candidates/add")
    public String addCandidate(
            @ModelAttribute("candidate") Candidate candidate,
            @RequestParam(value = "skills", required = false) List<Long> skillIds,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("countries", CountryCode.values());
            model.addAttribute("allSkills", skillRepository.findAll());
            return "candidates/add";
        }

        addressRepository.save(candidate.getAddress());

        if (skillIds != null && !skillIds.isEmpty()) {
            Set<CandidateSkill> candidateSkills = new LinkedHashSet<>();
            for (Long skillId : skillIds) {
                Skill skill = skillRepository.findById(skillId).orElse(null);
                if (skill != null) {
                    CandidateSkill candidateSkill = new CandidateSkill();
                    candidateSkill.setCan(candidate);
                    candidateSkill.setSkill(skill);
                    candidateSkills.add(candidateSkill);
                }
            }
            candidate.setCandidateSkills(candidateSkills);
        }

        candidateRepository.save(candidate);

        return "redirect:/candidates";
    }


    @GetMapping("/show-edit-form/{id}")
    public ModelAndView edit(@PathVariable("id") long id) {
        ModelAndView modelAndView = new ModelAndView();
        Optional<Candidate> opt = candidateRepository.findById(id);
        if (opt.isPresent()) {
            Candidate candidate = opt.get();
            modelAndView.addObject("candidate", candidate);
            modelAndView.addObject("countries", CountryCode.values());

            modelAndView.setViewName("candidates/update");
        } else {
            modelAndView.setViewName("redirect:/candidates?error=candidateNotFound");
        }
        return modelAndView;
    }

    @PostMapping("/update")
    public String update(
            @ModelAttribute("candidate") Candidate candidate,
            BindingResult result,
            Model model) {

        if (candidate.getAddress() == null || candidate.getAddress().getCountry() == null) {
            model.addAttribute("error", "Country is required.");
            model.addAttribute("candidate", candidate);
            model.addAttribute("countries", CountryCode.values());
            return "candidates/update";
        }

        Address address = candidate.getAddress();
        if (address.getId() == null) {
            addressRepository.save(address);
        } else {
            addressRepository.save(address);
        }
        candidate.setAddress(address);
        candidateRepository.save(candidate);

        return "redirect:/candidates?success=updateSuccess";
    }


    @GetMapping("/suggest-skill-to-learn/{id}")
    public String suggestSkillToLearn(@PathVariable Long id, Model model) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid candidate Id:" + id));

        model.addAttribute("candidate", candidate);
        model.addAttribute("suggestedSkills", candidateServices.suggestTopSkills(id));
        return "candidates/suggestedskill";
    }

    @GetMapping("/profile/{id}")
    public ModelAndView viewCandidate(@PathVariable("id") long id) {
        ModelAndView modelAndView = new ModelAndView();
        Optional<Candidate> opt = candidateRepository.findById(id);
        if (opt.isPresent()) {
            Candidate candidate = opt.get();
            modelAndView.addObject("candidate", candidate);
            modelAndView.addObject("countries", CountryCode.values());

            modelAndView.setViewName("candidates/profile");
        } else {
            modelAndView.setViewName("redirect:/candidates?error=candidateNotFound");
        }
        return modelAndView;
    }

    @GetMapping("/search")
    public String searchCandidates(@RequestParam("keyword") String keyword,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model) {
        Page<Candidate> candidatePage = candidateServices.searchCandidates(keyword, page, size);
        model.addAttribute("candidatePage", candidatePage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageNumbers", IntStream.rangeClosed(1, candidatePage.getTotalPages()).boxed().collect(Collectors.toList()));
        return "candidates/list";
    }

    @GetMapping("/suggest-jobs/{id}")
    public String suggestJobs(@PathVariable("id") Long candidateId, Model model) {
        List<Job> suggestedJobs = candidateServices.suggestJobsForCandidate(candidateId);
        model.addAttribute("suggestedJobs", suggestedJobs);
        return "candidates/suggested_jobs";
    }

}