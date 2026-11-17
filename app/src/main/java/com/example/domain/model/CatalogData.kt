package com.example.domain.model

object CatalogData {

    val indoorAdvisories: List<IndoorAdvisoryItem> = listOf(
        IndoorAdvisoryItem(
            id = "hepa",
            title = "True HEPA (H13) Air Purifiers",
            category = "Filtration",
            budgetTag = "Medium–High Budget",
            benefit = "Captures ≥99.95% of airborne PM2.5 particles, winter smog haze, and fine dust inside closed bedrooms when sized properly (CADR matching room area).",
            limitation = "Does not remove gaseous pollutants (CO, excess NO₂) unless paired with substantial activated carbon, and loses effectiveness if windows remain open.",
            actionSteps = listOf(
                "Run continuously on Medium/Auto in bedrooms overnight during AQI > 100.",
                "Place at least 30 cm away from walls and curtains for unobstructed airflow.",
                "Avoid ionizer or ozone-producing modes, which can irritate airways."
            )
        ),
        IndoorAdvisoryItem(
            id = "ventilation",
            title = "Timed Cross-Ventilation",
            category = "Ventilation",
            budgetTag = "Zero Cost",
            benefit = "Flushes accumulated indoor CO₂, cooking moisture, and VOCs when outdoor air is relatively cleaner (typically mid-afternoon 1 PM–4 PM when inversion lifts).",
            limitation = "Opening windows during winter early-morning or late-night smog peaks pulls high PM2.5 directly indoors.",
            actionSteps = listOf(
                "Check live US AQI on AirCare Monitor before opening windows.",
                "Ventilate briefly (15–20 mins) during afternoon dispersion hours when outdoor AQI is lower than indoor haze.",
                "Skip natural ventilation completely when outdoor AQI exceeds 200."
            )
        ),
        IndoorAdvisoryItem(
            id = "window_seal",
            title = "Close Windows & Seal Drafts During Peaks",
            category = "Containment",
            budgetTag = "Low Cost",
            benefit = "Reduces infiltration of outdoor winter smog and traffic particulates during rush-hour and nocturnal temperature inversions.",
            limitation = "Prolonged airtight sealing without mechanical filtration can raise indoor CO₂ and humidity levels.",
            actionSteps = listOf(
                "Keep roadside-facing windows closed from 6–10 AM and 6–11 PM on high-AQI days.",
                "Use foam weather-stripping or rolled cotton draft stoppers under exterior doors."
            )
        ),
        IndoorAdvisoryItem(
            id = "wet_mop",
            title = "Wet Mopping & Damp Dusting",
            category = "Cleaning",
            budgetTag = "Zero Cost",
            benefit = "Traps settled PM10, road grit, and allergens on hard floors and shelves without kicking fine dust back into breathing zones.",
            limitation = "Requires daily consistency in dusty urban neighborhoods and does not capture airborne suspended PM2.5.",
            actionSteps = listOf(
                "Replace dry brooming and feather dusters with microfiber damp cloths and wet mops.",
                "Wipe window sills and entry mats first after heavy traffic hours."
            )
        ),
        IndoorAdvisoryItem(
            id = "no_combustion",
            title = "Eliminate Indoor Combustion & Smoke",
            category = "Source Control",
            budgetTag = "Zero Cost",
            benefit = "Prevents acute indoor PM2.5 spikes that can exceed 300 µg/m³ from incense (agarbatti), dhoop, mosquito coils, candles, or tobacco smoke.",
            limitation = "Requires household habit changes during cultural routines or mosquito seasons.",
            actionSteps = listOf(
                "Use window mesh screens or electric liquid vaporizers/nets instead of burning mosquito coils.",
                "Move any ceremonial lamps/incense outdoors or to a well-exhausted balcony.",
                "Enforce a strict smoke-free indoor home policy."
            )
        ),
        IndoorAdvisoryItem(
            id = "kitchen_exhaust",
            title = "Kitchen Chimney & Cooking Exhaust",
            category = "Kitchen",
            budgetTag = "Low–Medium Budget",
            benefit = "Vents frying aerosols (PM2.5) and gas-stove NO₂ directly outdoors before they spread into bedrooms and living spaces.",
            limitation = "Recirculating ductless hoods without clean charcoal/grease filters push gases back into the room.",
            actionSteps = listOf(
                "Switch on the exhaust fan or ducted chimney 2 minutes before lighting the stove and leave it on 10 minutes after.",
                "Cover pots with lids while boiling or simmering to reduce steam and ultrafine particles."
            )
        ),
        IndoorAdvisoryItem(
            id = "filter_maintenance",
            title = "Purifier Placement & Filter Maintenance",
            category = "Filtration",
            budgetTag = "Low Cost",
            benefit = "Maintains rated Clean Air Delivery Rate (CADR) and prevents clogged pre-filters from straining the purifier motor.",
            limitation = "HEPA filters cannot be washed with water (unless explicitly labeled washable); wet-washing ruins the fiber electrostatic charge.",
            actionSteps = listOf(
                "Vacuum or gently brush the outer mesh pre-filter every 2 weeks during winter smog.",
                "Replace the main HEPA cartridge every 6–12 months based on usage hours and discoloration."
            )
        ),
        IndoorAdvisoryItem(
            id = "diy_corsirosenthal",
            title = "Low-Cost DIY Box Fan + MERV-13 Filter",
            category = "Low-Cost DIY",
            budgetTag = "Low Budget",
            benefit = "Delivers high particulate filtration airflow at a fraction of commercial purifier prices when sealed cleanly to a box fan.",
            limitation = "Louder than branded sleep-mode purifiers and requires safe electrical cord placement away from small children.",
            actionSteps = listOf(
                "Tape a MERV-13 (or HEPA-grade) pleated HVAC filter tightly to the intake side of a standard box fan with airflow arrow pointing into the fan.",
                "Never leave improvised fan setups unattended overnight if wiring is worn."
            )
        )
    )

    val sustainableAlternatives: List<SustainableAlternative> = listOf(
        SustainableAlternative(
            id = "public_transit",
            title = "Metro Rail & City Public Bus",
            category = "Commute",
            practicalBenefits = listOf(
                "Cuts per-passenger tailpipe emissions sharply compared to solo petrol/diesel cars.",
                "Reduces daily parking stress and fuel expenses on congested urban corridors.",
                "Metro systems run on electricity, shifting tailpipe exhaust out of dense street canyons."
            ),
            emissionsNote = "Typical range: ~35–68 g CO₂e/passenger-km vs ~171 g/km for solo petrol cars.",
            aqiSuitabilityHint = "Recommended even on elevated AQI days; wear a mask during station walks if AQI > 150."
        ),
        SustainableAlternative(
            id = "carpooling",
            title = "Neighborhood & Office Carpooling",
            category = "Commute",
            practicalBenefits = listOf(
                "Splits fuel and toll expenses among 2–4 neighbors or coworkers.",
                "Directly takes 1–3 private cars off peak-hour bottlenecks, reducing local idling exhaust.",
                "Safer, enclosed option for children or seniors during unhealthy smog mornings."
            ),
            emissionsNote = "Sharing a car among 3 riders reduces per-person emissions by ~67%.",
            aqiSuitabilityHint = "Great option when AQI > 150 and walking/cycling exposure should be minimized."
        ),
        SustainableAlternative(
            id = "active_travel",
            title = "Walking & Cycling (When AQI Permits)",
            category = "Active Mobility",
            practicalBenefits = listOf(
                "Zero tailpipe emissions and zero fuel cost for short 1–5 km neighborhood errands.",
                "Boosts cardiovascular fitness and reduces street-level congestion around school gates."
            ),
            emissionsNote = "0 g tailpipe CO₂/km.",
            aqiSuitabilityHint = "Best when US AQI ≤ 100. Between 101–150, keep routes away from heavy diesel roads; above 150, switch to enclosed transit."
        ),
        SustainableAlternative(
            id = "ev_mobility",
            title = "Electric 2-Wheelers, E-Autos & EVs",
            category = "Clean Mobility",
            practicalBenefits = listOf(
                "Eliminates street-level tailpipe NO₂, CO, and exhaust particulate emissions.",
                "Lower per-kilometer running and maintenance costs for daily urban commuters."
            ),
            emissionsNote = "Lifecycle CO₂ depends on local grid electricity mix, typically much lower than petrol vehicles.",
            aqiSuitabilityHint = "Use N95 mask on 2-wheelers whenever US AQI > 100 due to ambient road dust."
        ),
        SustainableAlternative(
            id = "trip_chaining",
            title = "Trip Chaining & Avoiding Short Cold-Start Trips",
            category = "Smart Planning",
            practicalBenefits = listOf(
                "Cold engines emit higher pollutants in the first 2–3 km before catalytic converters warm up.",
                "Combining grocery, pharmacy, and school errands into one loop saves fuel and time."
            ),
            emissionsNote = "Eliminating 2 short car trips/week avoids ~30–60 kg CO₂e annually plus high cold-start NOx.",
            aqiSuitabilityHint = "Always helpful regardless of AQI level."
        ),
        SustainableAlternative(
            id = "efficient_appliances",
            title = "Energy-Efficient Appliances & Clean Cooking",
            category = "Home Energy",
            practicalBenefits = listOf(
                "BLDC ceiling fans and inverter appliances cut household electricity bills by 30–50%.",
                "Electric induction or properly vented LPG reduces indoor NO₂ compared to solid fuels."
            ),
            emissionsNote = "Lower household kWh demand reduces peak fossil power plant load.",
            aqiSuitabilityHint = "Clean electric cooking helps keep indoor PM2.5/NO₂ safer during sealed-window smog days."
        ),
        SustainableAlternative(
            id = "no_waste_burning",
            title = "Zero Open Waste & Leaf Burning",
            category = "Zero Burning",
            practicalBenefits = listOf(
                "Open burning of dry leaves, plastics, and trash releases toxic dioxins, black carbon, and extreme local PM2.5.",
                "Composting garden leaves returns nutrients to community parks and trees."
            ),
            emissionsNote = "Prevents acute hyper-local smoke plumes that spike neighborhood AQI overnight.",
            aqiSuitabilityHint = "Critical during calm winter evenings when smoke lingers near ground level."
        ),
        SustainableAlternative(
            id = "community_action",
            title = "Community Clean-Air & Dust-Control Practices",
            category = "Community",
            practicalBenefits = listOf(
                "Coordinated school drop-off zones cut idling exhaust outside classrooms.",
                "Tree-care watering and mulching bare soil patches reduce windblown PM10 road dust."
            ),
            emissionsNote = "Multiplies individual household habits across apartment blocks and neighborhoods.",
            aqiSuitabilityHint = "Schedule outdoor tree-care watering during lower-AQI afternoon windows."
        )
    )

    val badges: List<Triple<String, String, Int>> = listOf(
        Triple("Fresh Starter", "Complete 1 daily clean-air action", 1),
        Triple("Indoor Guardian", "Complete 3 daily clean-air actions", 3),
        Triple("Transit Ally", "Complete 5 daily clean-air actions", 5),
        Triple("7-Day Clean Air Champion", "Finish all 7 days of the challenge", 7)
    )
}
