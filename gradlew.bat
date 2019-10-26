<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:ns1="http://schemas.android.com/tools">
    <color name="design_bottom_navigation_shadow_color">#14000000</color>
    <color name="design_fab_shadow_end_color">@android:color/transparent</color>
    <color name="design_fab_shadow_mid_color">#14000000</color>
    <color name="design_fab_shadow_start_color">#44000000</color>
    <color name="design_fab_stroke_end_inner_color">#0A000000</color>
    <color name="design_fab_stroke_end_outer_color">#0F000000</color>
    <color name="design_fab_stroke_top_inner_color">#1AFFFFFF</color>
    <color name="design_fab_stroke_top_outer_color">#2EFFFFFF</color>
    <color name="design_snackbar_background_color">#323232</color>
    <declare-styleable name="AppBarLayout"><attr name="elevation"/><attr name="android:background"/><attr format="boolean" name="expanded"/><attr name="android:keyboardNavigationCluster"/><attr name="android:touchscreenBlocksFocus"/></declare-styleable>
    <declare-styleable name="AppBarLayoutStates"><attr format="boolean" name="state_collapsed"/><attr format="boolean" name="state_collapsible"/></declare-styleable>
    <declare-styleable name="AppBarLayout_Layout"><attr name="layout_scrollFlags">
            
            <flag name="scroll" value="0x1"/>

            
            <flag name="exitUntilCollapsed" value="0x2"/>

            
            <flag name="enterAlways" value="0x4"/>

            
            <flag name="enterAlwaysCollapsed" value="0x8"/>

            
            <flag name="snap" value="0x10"/>
        </attr><attr format="reference" name="layout_scrollInterpolator"/></declare-styleable>
    <declare-styleable name="BottomNavigationView"><attr name="menu"/><attr name="itemIconTint"/><attr name="itemTextColor"/><attr name="itemBackground"/><attr name="elevation"/></declare-styleable>
    <declare-styleable name="BottomSheetBehavior_Layout"><attr format="dimension" name="behavior_peekHeight">
            
            <enum name="auto" value="-1"/>
        </attr><attr format="boolean" name="behavior_hideable"/><attr format="boolean" name="behavior_skipCollapsed"/></declare-styleable>
    <declare-styleable name="CollapsingToolbarLayout"><attr format="dimension" name="expandedTitleMar