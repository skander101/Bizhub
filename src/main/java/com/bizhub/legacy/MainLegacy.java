package com.bizhub.legacy;

import com.bizhub.legacy.model.Avis;
import com.bizhub.legacy.model.Formation;
import com.bizhub.legacy.model.User;
import com.bizhub.legacy.service.ServiceAvis;
import com.bizhub.legacy.service.ServiceFormation;
import com.bizhub.legacy.service.ServiceUser;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class MainLegacy {
    public static void main(String[] args) {
        ServiceUser serviceUser = new ServiceUser();
        ServiceFormation serviceFormation = new ServiceFormation();
        ServiceAvis serviceAvis = new ServiceAvis();

        // We create 2 users: a trainer (formateur) and a reviewer (startup).
        User trainer = new User(
                "trainer.demo." + System.currentTimeMillis() + "@example.com",
                "hash123",
                "formateur",
                "Demo Trainer"
        );

        User reviewer = new User(
                "reviewer.demo." + System.currentTimeMillis() + "@example.com",
                "hash123",
                "startup",
                "Demo Reviewer"
        );

        try {
            // --- USER CRUD (minimal) ---
            serviceUser.add(trainer);
            serviceUser.add(reviewer);
            System.out.println("Created trainer: " + trainer);
            System.out.println("Created reviewer: " + reviewer);

            trainer.setPhone("22110000");
            serviceUser.update(trainer);
            System.out.println("Updated trainer: " + serviceUser.getById(trainer.getUserId()));

            // --- FORMATION CRUD ---
            Formation formation = new Formation(
                    "Java JDBC Bootcamp",
                    trainer.getUserId(),
                    LocalDate.now().plusDays(7),
                    LocalDate.now().plusDays(10)
            );
            formation.setDescription("Learn JDBC CRUD with MySQL/MariaDB");
            formation.setCost(new BigDecimal("199.00"));

            serviceFormation.add(formation);
            System.out.println("Created formation: " + formation);

            Formation dbFormation = serviceFormation.getById(formation.getFormationId());
            System.out.println("Read formation: " + dbFormation);

            dbFormation.setCost(new BigDecimal("149.00"));
            dbFormation.setTitle("Java JDBC Bootcamp (Updated)");
            serviceFormation.update(dbFormation);
            System.out.println("Updated formation: " + serviceFormation.getById(dbFormation.getFormationId()));

            System.out.println("All formations: " + serviceFormation.getAll());

            // --- AVIS CRUD ---
            // unique constraint: (reviewer_id, formation_id)
            Avis avis = new Avis(reviewer.getUserId(), formation.getFormationId(), 5);
            avis.setComment("Very useful formation!");
            avis.setVerified(true);

            serviceAvis.add(avis);
            System.out.println("Created avis: " + avis);

            Avis dbAvis = serviceAvis.getById(avis.getAvisId());
            System.out.println("Read avis: " + dbAvis);

            dbAvis.setRating(4);
            dbAvis.setComment("Updated: Good and practical.");
            serviceAvis.update(dbAvis);
            System.out.println("Updated avis: " + serviceAvis.getById(dbAvis.getAvisId()));

            System.out.println("Avis by formation: " + serviceAvis.getByFormationId(formation.getFormationId()));
            System.out.println("All avis: " + serviceAvis.getAll());

            // --- DELETE ORDER (respect FKs) ---
            // avis -> formation -> users
            serviceAvis.delete(dbAvis);
            System.out.println("Deleted avis id=" + dbAvis.getAvisId());

            serviceFormation.delete(dbFormation);
            System.out.println("Deleted formation id=" + dbFormation.getFormationId());

            serviceUser.delete(reviewer);
            serviceUser.delete(trainer);
            System.out.println("Deleted users trainer/reviewer");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
