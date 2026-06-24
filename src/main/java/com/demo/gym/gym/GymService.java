package com.demo.gym.gym;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GymService {

    private final GymRepository gymRepository;

    @Transactional
    public Gym saveGym(Gym gym) {
        if (gymRepository.existsByTaxId(gym.getTaxId())) {
            throw new IllegalArgumentException("Gym with Tax ID " + gym.getTaxId() + " already exists.");
        }
        return gymRepository.save(gym);
    }

    @Transactional(readOnly = true)
    public Gym getGym(Long id) {
        return gymRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gym not found with id: " + id));
    }

    @Transactional
    public Gym updateGym(Long id, Gym updates) {
        Gym gym = gymRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gym not found with id: " + id));

        if (updates.getName() != null) gym.setName(updates.getName());
        if (updates.getEmail() != null) gym.setEmail(updates.getEmail());
        if (updates.getPhoneNumber() != null) gym.setPhoneNumber(updates.getPhoneNumber());
        if (updates.getWebsiteUrl() != null) gym.setWebsiteUrl(updates.getWebsiteUrl());
        if (updates.getCapacity() != null) gym.setCapacity(updates.getCapacity());
        if (updates.getAddress() != null) gym.setAddress(updates.getAddress());

        return gymRepository.save(gym);
    }

    @Transactional
    public boolean deleteGym(Long id) {
        if (!gymRepository.existsById(id)) {
            return false;
        }
        gymRepository.deleteById(id);
        return true;
    }
}
