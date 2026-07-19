package com.picker.BlinkitPicker.Repository;

import com.picker.BlinkitPicker.Model.StoreModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreRepo extends JpaRepository<StoreModel, String> {
    Optional<StoreModel> findByStoreId(String storeId);
}
