package com.wivernz.itera.core.designsystem.component

import com.wivernz.itera.R
import com.wivernz.itera.domain.model.Skill

val Skill.title: Int get() = when (this) {
    Skill.FOCUS -> R.string.skill_focus
    Skill.PLANNING -> R.string.skill_planning
    Skill.LEARNING -> R.string.skill_learning
    Skill.HABITS -> R.string.skill_habits
    Skill.REFLECTION -> R.string.skill_reflection
}

val Skill.description: Int get() = when (this) {
    Skill.FOCUS -> R.string.skill_focus_desc
    Skill.PLANNING -> R.string.skill_planning_desc
    Skill.LEARNING -> R.string.skill_learning_desc
    Skill.HABITS -> R.string.skill_habits_desc
    Skill.REFLECTION -> R.string.skill_reflection_desc
}
