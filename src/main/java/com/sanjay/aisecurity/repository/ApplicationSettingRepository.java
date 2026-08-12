package com.sanjay.aisecurity.repository;

import com.sanjay.aisecurity.entity.ApplicationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link ApplicationSetting} entity.
 *
 * <p>Provides data access to platform configuration key-value settings.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Repository
public interface ApplicationSettingRepository extends JpaRepository<ApplicationSetting, Long> {

    Optional<ApplicationSetting> findBySettingKey(String settingKey);

    List<ApplicationSetting> findByCategory(String category);

    List<ApplicationSetting> findAllByOrderByCategoryAscSettingKeyAsc();

    boolean existsBySettingKey(String settingKey);
}
