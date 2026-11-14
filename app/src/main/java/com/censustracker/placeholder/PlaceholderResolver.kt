package com.censustracker.placeholder

import com.censustracker.domain.model.CollectorProfile
import com.censustracker.domain.model.Contact

class PlaceholderResolver(
    private val collectorProfile: CollectorProfile?,
    private val contact: Contact?
) {

    fun resolve(template: String): String {
        var result = template

        // Collector placeholders ({collector.*} is canonical; {teacher.*} kept for legacy templates)
        collectorProfile?.let { profile ->
            result = result.replace("{teacher.name}", profile.name)
            result = result.replace("{teacher.phone}", profile.phoneNumber)
            result = result.replace("{teacher.email}", profile.email ?: "")
            result = result.replace("{teacher.link}", profile.referenceLink ?: "")

            result = result.replace("{collector.name}", profile.name)
            result = result.replace("{collector.phone}", profile.phoneNumber)
            result = result.replace("{collector.email}", profile.email ?: "")
            result = result.replace("{collector.link}", profile.referenceLink ?: "")
        }

        // Respondent placeholders
        contact?.let { c ->
            result = result.replace("{respondent.name}", c.name)
            result = result.replace("{respondent.phone}", c.phoneNumber)
            result = result.replace("{respondent.houseNo}", c.houseNo ?: "")
            result = result.replace("{respondent.location}", c.location ?: "")
            result = result.replace("{respondent.email}", c.email ?: "")
        }

        return result
    }

    companion object {
        fun availablePlaceholders(): List<String> =
            listOf(
                "{collector.name}",
                "{collector.phone}",
                "{collector.email}",
                "{collector.link}",
                "{respondent.name}",
                "{respondent.phone}",
                "{respondent.houseNo}",
                "{respondent.location}",
                "{respondent.email}"
            )
    }
}