package com.openclassrooms.go4lunch.database;

import androidx.annotation.VisibleForTesting;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.openclassrooms.go4lunch.service.places.response.details.ClosingHours;
import com.openclassrooms.go4lunch.service.places.response.details.OpeningHours;

/**
 * Data class to store in a row of the @{@link Go4LunchDatabase} hours_table table.
 */
@Entity(tableName = "hours_table")
public class HoursData {

    @PrimaryKey(autoGenerate = true) public int id;

    @ColumnInfo(name = "restaurant_id") private final String restaurantId;

    @Embedded public final ClosingHours closingHours;

    @Embedded public final OpeningHours openingHours;

    public HoursData(ClosingHours closingHours, OpeningHours openingHours, String restaurantId) {
        this.closingHours = closingHours;
        this.openingHours = openingHours;
        this.restaurantId = restaurantId;
    }

    // Getters
    public int getId() { return id; }

    public String getRestaurantId() { return restaurantId; }

    public void setId(int id) { this.id = id; }

    /**
     * Used for testing only. See HoursDataUnitTest.java file.
     */
    @VisibleForTesting
    public OpeningHours getOpeningHours() {
        return openingHours;
    }

    /**
     * Used for testing only. See HoursDataUnitTest.java file.
     */
    @VisibleForTesting
    public ClosingHours getClosingHours() {
        return closingHours;
    }
}
