package com.keepSafe911.model

import com.keepSafe911.R

class MapFilter(){

    var mapFilterId: Int = 0
    var fabBackground: Int = 0
    var fabIcon: Int = 0
    var filterName: Int = 0
    var termsName: String = ""
    var category: String = ""
    var isSelected: Boolean = false

    constructor(mapFilterId: Int, fabIcon: Int,filterName: Int,termsName: String, isSelected: Boolean, category: String) : this(){
        this.mapFilterId = mapFilterId
        this.termsName = termsName
        this.filterName = filterName
        this.fabIcon = fabIcon
        this.isSelected = isSelected
        this.category = category
    }
    constructor(mapFilterId: Int, fabBackground: Int, fabIcon: Int,filterName: Int,termsName: String, isSelected: Boolean, category: String) : this() {
        this.mapFilterId = mapFilterId
        this.termsName = termsName
        this.filterName = filterName
        this.fabIcon = fabIcon
        this.fabBackground = fabBackground
        this.isSelected = isSelected
        this.category = category
    }

    companion object{
        val getMapFilter: ArrayList<MapFilter>
        get() {
            val mapFilter =  ArrayList<MapFilter>()
            mapFilter.add(MapFilter(1,R.color.special_green, R.drawable.local_hotel, R.string.str_hotel,"hotels",false,""))
            mapFilter.add(MapFilter(2,R.color.color_red, R.drawable.local_restaurant, R.string.str_restaurant,"Restaurants",false,""))
            mapFilter.add(MapFilter(3,R.color.color_light_blue, R.drawable.local_theater, R.string.str_theater,"Cinema Theater",false,""))
            mapFilter.add(MapFilter(4,R.color.caldroid_holo_blue_dark, R.drawable.local_gas_station, R.string.str_gas,"Gas Station Near Me",false,""))
//            mapFilter.add(MapFilter(5,R.color.caldroid_dark_yellow, R.drawable.local_bank, R.string.str_bank,"banks",false,""))
            mapFilter.add(MapFilter(6,R.color.color_light_purple, R.drawable.local_atm, R.string.str_atm,"atm",false,""))
            mapFilter.add(MapFilter(7,R.color.medium_blue, R.drawable.local_hospital, R.string.str_hospital,"Clinic",false,""))
            mapFilter.add(MapFilter(8,R.color.dark_purple, R.drawable.local_auto_mechanic, R.string.str_auto_mechanic,"Auto Mechanic",false,""))
            mapFilter.add(MapFilter(9,R.color.dark_pink, R.drawable.local_airport, R.string.str_airport,"airports",false,"airports"))
            mapFilter.add(MapFilter(10,R.color.light_green, R.drawable.local_museum, R.string.str_museum,"Museums Near Me",false,""))
            mapFilter.add(MapFilter(11,R.color.light_purple, R.drawable.local_urgent_care, R.string.str_urgent_care,"Urgent Care Walk in Clinic",false,""))
            mapFilter.add(MapFilter(12,R.color.light_office, R.drawable.local_office, R.string.str_physician_office,"Physician",false,""))
            mapFilter.add(MapFilter(13,R.color.pet_color, R.drawable.local_pet_hospital, R.string.str_veterinary_hospital,"Veterinary Hospital",false,""))
            mapFilter.add(MapFilter(14,R.color.police_color, R.drawable.local_police_station, R.string.str_police_station,"Police Station",false,""))
            mapFilter.add(MapFilter(15,R.color.landmark_color, R.drawable.local_landmark, R.string.str_landmark,"Landmark",false,""))
            return mapFilter
        }

        val getMapFilterImageName: ArrayList<MapFilter>
        get() {
            val mapFilter =  ArrayList<MapFilter>()
            mapFilter.add(MapFilter(1,R.drawable.ic_filter_hotel, R.string.str_hotel,"hotels",false,""))
            mapFilter.add(MapFilter(2,R.drawable.ic_filter_restaurant, R.string.str_restaurant,"Restaurants",false,""))
            mapFilter.add(MapFilter(3,R.drawable.ic_filter_theater, R.string.str_theater,"Cinema Theater",false,""))
            mapFilter.add(MapFilter(4,R.drawable.ic_filter_gas, R.string.str_gas,"Gas Station Near Me",false,""))
            mapFilter.add(MapFilter(5,R.drawable.ic_filter_bank, R.string.str_bank,"banks",false,""))
            mapFilter.add(MapFilter(6,R.drawable.ic_filter_atm, R.string.str_atm,"atm",false,""))
            mapFilter.add(MapFilter(7,R.drawable.ic_filter_hospital, R.string.str_hospital,"Clinic",false,""))
            mapFilter.add(MapFilter(8,R.drawable.ic_filter_atm, R.string.str_auto_mechanic,"Auto Mechanic",false,""))
            mapFilter.add(MapFilter(9,R.drawable.ic_filter_atm, R.string.str_airport,"airports",false,"airports"))
            mapFilter.add(MapFilter(10,R.drawable.ic_filter_atm, R.string.str_museum,"Museums Near Me",false,""))
            mapFilter.add(MapFilter(11,R.drawable.ic_filter_atm, R.string.str_urgent_care,"Urgent Care Walk in Clinic",false,""))
            mapFilter.add(MapFilter(12,R.drawable.ic_filter_atm, R.string.str_physician_office,"Physician",false,""))
            mapFilter.add(MapFilter(13,R.drawable.ic_filter_atm, R.string.str_veterinary_hospital,"Veterinary Hospital",false,""))
            mapFilter.add(MapFilter(14,R.drawable.ic_filter_atm, R.string.str_police_station,"Police Station",false,""))
            mapFilter.add(MapFilter(15,R.drawable.local_landmark, R.string.str_landmark,"Landmark",false,""))
            return mapFilter
        }
    }
}