package com.snaptechnology.bgonzalez.controllers;

import com.snaptechnology.bgonzalez.model.Location;
import com.snaptechnology.bgonzalez.services.LocationService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * LocationController is a controller to manipulate request of location,
 * In this project the request are going to be from a devices android
 *
 * @author Brayan González Chaves
 * @since 19/09/2016
 *
 */
@RestController
@RequestMapping("/location")
public class LocationController {

    @Autowired
    LocationService locationService ;

    /**
     * Request Method POST to get all locations using the restful controller
     * @return list of Locations
     */

    @RequestMapping("/findall")
    public
    List<Location> findAll(){
        JSONArray jsonArray = new JSONArray();

        locationService.getAllLocations().forEach(jsonArray::put);
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("locations", jsonArray);
        return locationService.getAllLocations() ;
    }
}
