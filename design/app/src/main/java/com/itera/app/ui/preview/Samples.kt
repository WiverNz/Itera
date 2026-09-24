package com.itera.app.ui.preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
enum class ComponentSample {
    ScreenColumn,
    IteraCard,
    Divider,
    Eyebrow,
    Pill,
    IteraButton,
    CircleIconButton,
    TopBar,
    TechniqueToken,
    ChoiceChip,
    Segmented,
    StepRow,
    StepDot,
    MasteryLadder,
    ProgressBar,
    NoteField,
    SectionTitle,
    CheckCircle,
    RadioDot,
    LinkRow,
    MasteryDots,
    IntervalLadder,
    AnimatedCheck,
    Group,
    ValueRow,
    SwitchRow,
    TimeRow,
    LanguageSheet,
    LanguagePill,
    TimePickerSheet,
    EmptyState,
    ErrorState,
    Skeleton
}
class Samples : PreviewParameterProvider<ComponentSample> {
    override val values = ComponentSample.entries.asSequence()
}
