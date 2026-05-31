package com.company.reimbursement.oa;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/oa-numbers")
@PreAuthorize("hasAnyRole('ADMIN','SPECIALIST')")
public class OaNumberController {
    private final OaNumberRepository oaNumbers;

    public OaNumberController(OaNumberRepository oaNumbers) {
        this.oaNumbers = oaNumbers;
    }

    @GetMapping
    List<OaNumber> list() {
        return oaNumbers.findAll();
    }

    @PostMapping
    OaNumber create(@RequestBody CreateOaRequest request) {
        return oaNumbers.save(OaNumber.create(request.number()));
    }

    @PutMapping("/{id}")
    OaNumber update(@PathVariable Long id, @RequestBody CreateOaRequest request) {
        OaNumber oa = oaNumbers.findById(id).orElseThrow();
        oa.setNumber(request.number());
        return oaNumbers.save(oa);
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id) {
        oaNumbers.deleteById(id);
    }

    public record CreateOaRequest(String number) {
    }
}
