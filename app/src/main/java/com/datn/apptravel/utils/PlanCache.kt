package com.datn.apptravels.utils

import com.datn.apptravels.data.model.Plan

/**
 * Simple in-memory cache for Plan objects
 * Used to pass full plan data between activities without Intent size limits
 */
object PlanCache {
    private val cache = mutableMapOf<String, Plan>()
    
    fun put(planId: String, plan: Plan) {
        cache[planId] = plan
    }
    
    fun get(planId: String): Plan? {
        return cache[planId]
    }
    
    fun remove(planId: String) {
        cache.remove(planId)
    }
    
    fun clear() {
        cache.clear()
    }
    
    fun putAll(plans: List<Plan>) {
        plans.forEach { plan ->
            plan.id?.let { id ->
                cache[id] = plan
            }
        }
    }
}
