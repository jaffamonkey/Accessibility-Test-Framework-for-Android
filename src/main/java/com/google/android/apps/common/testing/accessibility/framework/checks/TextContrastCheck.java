/*
 * Copyright (C) 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.common.testing.accessibility.framework.checks;

import static com.google.android.apps.common.testing.accessibility.framework.ViewHierarchyElementUtils.isPotentiallyObscured;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheck.Category;
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType;
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheck;
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheckResult;
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheckResultWithImage;
import com.google.android.apps.common.testing.accessibility.framework.HashMapResultMetadata;
import com.google.android.apps.common.testing.accessibility.framework.Parameters;
import com.google.android.apps.common.testing.accessibility.framework.QuestionHandler;
import com.google.android.apps.common.testing.accessibility.framework.ResultMetadata;
import com.google.android.apps.common.testing.accessibility.framework.ViewHierarchyElementUtils;
import com.google.android.apps.common.testing.accessibility.framework.replacements.Rect;
import com.google.android.apps.common.testing.accessibility.framework.replacements.Span;
import com.google.android.apps.common.testing.accessibility.framework.replacements.SpannableString;
import com.google.android.apps.common.testing.accessibility.framework.replacements.Spans;
import com.google.android.apps.common.testing.accessibility.framework.replacements.TextUtils;
import com.google.android.apps.common.testing.accessibility.framework.strings.StringManager;
import com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchy;
import com.google.android.apps.common.testing.accessibility.framework.uielement.ViewHierarchyElement;
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.Color;
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.ContrastSwatch;
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.ContrastUtils;
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.Image;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Check that ensures text content has sufficient contrast against its background */
public class TextContrastCheck extends AccessibilityHierarchyCheck {

  /** Result when the view is not visible. */
  public static final int RESULT_ID_NOT_VISIBLE = 1;
  /** Result when the view is not a {@link TextView}. */
  public static final int RESULT_ID_NOT_TEXT_VIEW = 2;
  /** Result when the {@link TextView} is empty. */
  public static final int RESULT_ID_TEXTVIEW_EMPTY = 3;
  /** Result when the view's text color could not be obtained. */
  public static final int RESULT_ID_COULD_NOT_GET_TEXT_COLOR = 4;
  /** Result when the view's background color could not be obtained. */
  public static final int RESULT_ID_COULD_NOT_GET_BACKGROUND_COLOR = 5;
  /** Legacy result when the view's text is not opaque. */
  public static final int RESULT_ID_TEXT_MUST_BE_OPAQUE = 6;
  /** Result when the view's background is not opaque. */
  public static final int RESULT_ID_BACKGROUND_MUST_BE_OPAQUE = 7;
  /** Result when the view's contrast is insufficient based on opaque text/background. */
  public static final int RESULT_ID_TEXTVIEW_CONTRAST_NOT_SUFFICIENT = 8;
  /** Result when the heuristic evaluation could not obtain a screenshot. */
  public static final int RESULT_ID_HEURISTIC_COULD_NOT_GET_SCREENCAPTURE = 9;
  /** Result when the view is not completely within the screenshot. */
  public static final int RESULT_ID_VIEW_NOT_WITHIN_SCREENCAPTURE = 10;
  /** Result when the view's contrast is insufficient based on heuristic evaluation. */
  public static final int RESULT_ID_TEXTVIEW_HEURISTIC_CONTRAST_NOT_SUFFICIENT = 11;
  /** Result when the view's contrast is between the small text ratio and the large text ratio. */
  public static final int RESULT_ID_TEXTVIEW_HEURISTIC_CONTRAST_BORDERLINE = 12;
  /** Result when the evaluated element was determined to be not enabled */
  public static final int RESULT_ID_NOT_ENABLED = 13;
  /** Result when the evaluated element's screen capture data was determined to be hidden */
  public static final int RESULT_ID_SCREENCAPTURE_DATA_HIDDEN = 14;
  /**
   * Result when the view's contrast is insufficient based on heuristic evaluation which uses
   * user-defined heuristic contrast ratio.
   */
  public static final int RESULT_ID_CUSTOMIZED_TEXTVIEW_HEURISTIC_CONTRAST_NOT_SUFFICIENT = 15;
  /** Result when the evaluated element's screen capture has a uniform color */
  public static final int RESULT_ID_SCREENCAPTURE_UNIFORM_COLOR = 16;
  /**
   * Result when the view's contrast is insufficient based on opaque text/background using
   * user-defined contrast ratio.
   */
  public static final int RESULT_ID_CUSTOMIZED_TEXTVIEW_CONTRAST_NOT_SUFFICIENT = 22;
  /**
   * Result when the view's upper bound of contrast ratio for a non-opqaue background is
   * insufficient.
   */
  public static final int RESULT_ID_TEXTVIEW_UPPER_BOUND_CONTRAST_NOT_SUFFICIENT = 23;
  /**
   * Result when the view's lower bound of contrast ratio for a non-opqaue background is
   * insufficient.
   */
  public static final int RESULT_ID_TEXTVIEW_LOWER_BOUND_CONTRAST_NOT_SUFFICIENT = 24;
  /**
   * Result when the view's upper bound of contrast ratio for a non-opqaue background is
   * insufficient using user-defined contrast ratio.
   */
  public static final int RESULT_ID_CUSTOMIZED_TEXTVIEW_UPPER_BOUND_CONTRAST_NOT_SUFFICIENT = 25;
  /**
   * Result when the view's lower bound of contrast ratio for a non-opqaue background is
   * insufficient using user-defined contrast ratio.
   */
  public static final int RESULT_ID_CUSTOMIZED_TEXTVIEW_LOWER_BOUND_CONTRAST_NOT_SUFFICIENT = 26;

  /** Result metadata key for the {@code int} color of the view's background. */
  public static final String KEY_BACKGROUND_COLOR = "KEY_BACKGROUND_COLOR";
  /** Result metadata key for the {@code int} (0-100)% opacity of the view's background. */
  public static final String KEY_BACKGROUND_OPACITY = "KEY_BACKGROUND_OPACITY";
  /** Result metadata key for the {@code double} computed contrast ratio of the view. */
  public static final String KEY_CONTRAST_RATIO = "KEY_CONTRAST_RATIO";
  /** Result metadata key for the {@code int} color of the view's foreground. */
  public static final String KEY_FOREGROUND_COLOR = "KEY_FOREGROUND_COLOR";
  /** Result metadata key for the {@code String} substring of text corresponding to the result. */
  public static final String KEY_RESULT_TEXT_SUBSTRING = "KEY_RESULT_TEXT_SUBSTRING";
  /** Result metadata key for the {@code double} required contrast ratio for this view. */
  public static final String KEY_REQUIRED_CONTRAST_RATIO = "KEY_REQUIRED_CONTRAST_RATIO";
  /**
   * Result metadata key for the {@code double} user-defined heuristic contrast ratio for this view.
   */
  public static final String KEY_CUSTOMIZED_HEURISTIC_CONTRAST_RATIO =
      "KEY_CUSTOMIZED_HEURISTIC_CONTRAST_RATIO";
  /** Result metadata key for the {@code String} bounds of the screenshot. */
  public static final String KEY_SCREENSHOT_BOUNDS_STRING = "KEY_SCREENSHOT_BOUNDS_STRING";
  /** Result metadata key for the {@code int} color of the view's text. */
  public static final String KEY_TEXT_COLOR = "KEY_TEXT_COLOR";
  /** Result metadata key for the {@code int} (0-100)% opacity of the view's text. */
  public static final String KEY_TEXT_OPACITY = "KEY_TEXT_OPACITY";
  /** Result metadata key for {@code double} required contrast ratio for large text. */
  public static final String KEY_TOLERANT_CONTRAST_RATIO = "KEY_TOLERANT_CONTRAST_RATIO";
  /** Result metadata key for the {@code String} bounds of the view. */
  public static final String KEY_VIEW_BOUNDS_STRING = "KEY_VIEW_BOUNDS_STRING";
  /**
   * Result metadata key for a {@code boolean} which is {@code true} iff the view is determined to
   * be touching the scrollable edge of a scrollable container.
   */
  public static final String KEY_IS_AGAINST_SCROLLABLE_EDGE = "KEY_IS_AGAINST_SCROLLABLE_EDGE";

  /** Result metadata key for the {@code ArrayList<String>} foreground colors of the view. */
  public static final String KEY_ADDITIONAL_FOREGROUND_COLORS = "KEY_ADDITIONAL_FOREGROUND_COLORS";
  /** Result metadata key for the {@code ArrayList<String>} computed contrast ratio. */
  public static final String KEY_ADDITIONAL_CONTRAST_RATIOS = "KEY_ADDITIONAL_CONTRAST_RATIOS";
  /** Result metadata key for whether the view may be obscured by other on-screen content. */
  public static final String KEY_IS_POTENTIALLY_OBSCURED = "KEY_IS_POTENTIALLY_OBSCURED";
  /**
   * Result metadata key for a {@code boolean} which is {@code true} iff the view's text size is
   * available and the text size is considered to be large.
   */
  public static final String KEY_IS_LARGE_TEXT = "KEY_IS_LARGE_TEXT";

  /** The amount by which a view's computed contrast ratio may fall below defined thresholds */
  public static final double CONTRAST_TOLERANCE = 0.01;

  private static final Class<? extends AccessibilityHierarchyCheck> CHECK_CLASS =
      TextContrastCheck.class;

  /** The constants must match the values in {@link android.graphics.Typeface}. */
  private static final int TYPEFACE_NORMAL = 0;

  private static final int TYPEFACE_BOLD = 1;

  private static final int OPAQUE_ALPHA = 255;

  @Override
  protected String getHelpTopic() {
    return "7158390"; // Color contrast
  }

  @Override
  public Category getCategory() {
    return Category.LOW_CONTRAST;
  }

  @Override
  public List<AccessibilityHierarchyCheckResult> runCheckOnHierarchy(
      AccessibilityHierarchy hierarchy,
      @Nullable ViewHierarchyElement fromRoot,
      @Nullable Parameters parameters) {
    List<AccessibilityHierarchyCheckResult> results = new ArrayList<>();
    List<? extends ViewHierarchyElement> viewsToEval = getElementsToEvaluate(fromRoot, hierarchy);
    for (ViewHierarchyElement view : viewsToEval) {
      if (!Boolean.TRUE.equals(view.isVisibleToUser())) {
        results.add(
            new AccessibilityHierarchyCheckResult(
                CHECK_CLASS,
                AccessibilityCheckResultType.NOT_RUN,
                view,
                RESULT_ID_NOT_VISIBLE,
                /* metadata= */ null));
        continue;
      }

      if (!view.checkInstanceOf(ViewHierarchyElementUtils.TEXT_VIEW_CLASS_NAME)
          || (view.checkInstanceOf(ViewHierarchyElementUtils.SWITCH_CLASS_NAME)
              && view.getTextCharacterLocations().isEmpty())) {
        // Only evaluate contrast for instances of TextView, or instances of Switch when character
        // locations are known.
        results.add(
            new AccessibilityHierarchyCheckResult(
                CHECK_CLASS,
                AccessibilityCheckResultType.NOT_RUN,
                view,
                RESULT_ID_NOT_TEXT_VIEW,
                /* metadata= */ null));
        continue;
      }

      if (TextUtils.isEmpty(view.getText()) && TextUtils.isEmpty(view.getHintText())) {
        results.add(
            new AccessibilityHierarchyCheckResult(
                CHECK_CLASS,
                AccessibilityCheckResultType.NOT_RUN,
                view,
                RESULT_ID_TEXTVIEW_EMPTY,
                /* metadata= */ null));
        continue;
      }

      if (!view.isEnabled()) {
        results.add(
            new AccessibilityHierarchyCheckResult(
                CHECK_CLASS,
                AccessibilityCheckResultType.NOT_RUN,
                view,
                RESULT_ID_NOT_ENABLED,
                /* metadata= */ null));
        continue;
      }

      ImmutableList<AccessibilityHierarchyCheckResult> lightweightResults =
          attemptLightweightEval(view, parameters);

      boolean runHeavyWeightEval = false;
      for (AccessibilityHierarchyCheckResult lightweightResult : lightweightResults) {
        if (lightweightResult.getResultId() == RESULT_ID_BACKGROUND_MUST_BE_OPAQUE) {
          AccessibilityHierarchyCheckResult contrastRangeResult =
              attemptContrastRangeEval(view, parameters, lightweightResult);
          if (contrastRangeResult != null) {
            if (contrastRangeResult.getType().equals(AccessibilityCheckResultType.WARNING)) {
              runHeavyWeightEval = true;
            } else {
              results.add(contrastRangeResult);
            }
          }
        } else if (lightweightResult.getType().equals(AccessibilityCheckResultType.NOT_RUN)) {
          results.add(lightweightResult);
          runHeavyWeightEval = true;
        } else {
          results.add(lightweightResult);
        }
      }
      if (runHeavyWeightEval) {
        AccessibilityHierarchyCheckResult heavyweightResult =
            attemptHeavyweightEval(view, parameters);
        if (heavyweightResult != null) {
          results.add(heavyweightResult);
        }
      }
    }

    return results;
  }

  @Override
  public String getMessageForResultData(
      Locale locale, int resultId, @Nullable ResultMetadata metadata) {
    String generated = generateMessageForResultId(locale, resultId);
    if (generated != null) {
      return generated;
    }

    // For each of the following result IDs, metadata will have been set on the result.
    checkNotNull(metadata);
    StringBuilder builder;
    switch (resultId) {
      case RESULT_ID_TEXT_MUST_BE_OPAQUE:
        builder =
            new StringBuilder()
                .append(StringManager.getString(locale, "result_message_text_must_be_opaque"))
                .append(" ")
                .append(
                    String.format(
                        locale,
                        StringManager.getString(
                            locale, "result_message_addendum_opacity_description"),
                        metadata.getFloat(KEY_TEXT_OPACITY)));
        return builder.toString();
      case RESULT_ID_BACKGROUND_MUST_BE_OPAQUE:
        builder =
            new StringBuilder()
                .append(StringManager.getString(locale, "result_message_background_must_be_opaque"))
                .append(" ")
                .append(
                    String.format(
                        locale,
                        StringManager.getString(
                            locale, "result_message_addendum_opacity_description"),
                        metadata.getFloat(KEY_BACKGROUND_OPACITY)));
        return builder.toString();
      case RESULT_ID_TEXTVIEW_CONTRAST_NOT_SUFFICIENT:
        builder =
            new StringBuilder(
                String.format(
                    locale,
                    StringManager.getString(
                        locale, "result_message_textview_contrast_not_sufficient"),
                    metadata.getDouble(KEY_CONTRAST_RATIO),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_TEXT_COLOR)),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_BACKGROUND_COLOR)),
                    metadata.getDouble(KEY_REQUIRED_CONTRAST_RATIO)));
        appendMetadataStringsToMessageIfNeeded(locale, metadata, builder);
        return builder.toString();
      case RESULT_ID_CUSTOMIZED_TEXTVIEW_CONTRAST_NOT_SUFFICIENT:
        builder =
            new StringBuilder(
                String.format(
                    locale,
                    StringManager.getString(
                        locale, "result_message_customized_textview_contrast_not_sufficient"),
                    metadata.getDouble(KEY_CONTRAST_RATIO),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_TEXT_COLOR)),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_BACKGROUND_COLOR)),
                    metadata.getDouble(KEY_CUSTOMIZED_HEURISTIC_CONTRAST_RATIO)));
        appendMetadataStringsToMessageIfNeeded(locale, metadata, builder);
        return builder.toString();
      case RESULT_ID_TEXTVIEW_UPPER_BOUND_CONTRAST_NOT_SUFFICIENT:
        builder =
            new StringBuilder(
                String.format(
                    locale,
                    StringManager.getString(
                        locale, "result_message_textview_upper_bound_contrast_not_sufficient"),
                    metadata.getDouble(KEY_CONTRAST_RATIO),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_TEXT_COLOR)),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_BACKGROUND_COLOR)),
                    metadata.getDouble(KEY_REQUIRED_CONTRAST_RATIO)));
        appendMetadataStringsToMessageIfNeeded(locale, metadata, builder);
        return builder.toString();
      case RESULT_ID_CUSTOMIZED_TEXTVIEW_UPPER_BOUND_CONTRAST_NOT_SUFFICIENT:
        builder =
            new StringBuilder(
                String.format(
                    locale,
                    StringManager.getString(
                        locale,
                        "result_message_customized_textview_upper_bound_contrast_not_sufficient"),
                    metadata.getDouble(KEY_CONTRAST_RATIO),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_TEXT_COLOR)),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_BACKGROUND_COLOR)),
                    metadata.getDouble(KEY_CUSTOMIZED_HEURISTIC_CONTRAST_RATIO)));
        appendMetadataStringsToMessageIfNeeded(locale, metadata, builder);
        return builder.toString();
      case RESULT_ID_TEXTVIEW_LOWER_BOUND_CONTRAST_NOT_SUFFICIENT:
        builder =
            new StringBuilder(
                String.format(
                    locale,
                    StringManager.getString(
                        locale, "result_message_textview_lower_bound_contrast_not_sufficient"),
                    metadata.getDouble(KEY_CONTRAST_RATIO),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_TEXT_COLOR)),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_BACKGROUND_COLOR)),
                    metadata.getDouble(KEY_REQUIRED_CONTRAST_RATIO)));
        appendMetadataStringsToMessageIfNeeded(locale, metadata, builder);
        return builder.toString();
      case RESULT_ID_CUSTOMIZED_TEXTVIEW_LOWER_BOUND_CONTRAST_NOT_SUFFICIENT:
        builder =
            new StringBuilder(
                String.format(
                    locale,
                    StringManager.getString(
                        locale,
                        "result_message_customized_textview_lower_bound_contrast_not_sufficient"),
                    metadata.getDouble(KEY_CONTRAST_RATIO),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_TEXT_COLOR)),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_BACKGROUND_COLOR)),
                    metadata.getDouble(KEY_CUSTOMIZED_HEURISTIC_CONTRAST_RATIO)));
        appendMetadataStringsToMessageIfNeeded(locale, metadata, builder);
        return builder.toString();
      case RESULT_ID_VIEW_NOT_WITHIN_SCREENCAPTURE:
        return String.format(
            locale,
            StringManager.getString(locale, "result_message_view_not_within_screencapture"),
            metadata.getString(KEY_VIEW_BOUNDS_STRING),
            metadata.getString(KEY_SCREENSHOT_BOUNDS_STRING));
      case RESULT_ID_TEXTVIEW_HEURISTIC_CONTRAST_NOT_SUFFICIENT:
        if (metadata.containsKey(KEY_IS_LARGE_TEXT)) {
          builder =
              new StringBuilder(
                  String.format(
                      locale,
                      StringManager.getString(
                          locale,
                          "result_message_textview_heuristic_contrast_not_sufficient_when_text_size_available"),
                      metadata.getDouble(KEY_CONTRAST_RATIO),
                      ContrastUtils.colorToHexString(metadata.getInt(KEY_FOREGROUND_COLOR)),
                      ContrastUtils.colorToHexString(metadata.getInt(KEY_BACKGROUND_COLOR)),
                      metadata.getDouble(KEY_REQUIRED_CONTRAST_RATIO)));
        } else {
          builder =
              new StringBuilder(
                  String.format(
                      locale,
                      StringManager.getString(
                          locale, "result_message_textview_heuristic_contrast_not_sufficient"),
                      metadata.getDouble(KEY_CONTRAST_RATIO),
                      ContrastUtils.colorToHexString(metadata.getInt(KEY_FOREGROUND_COLOR)),
                      ContrastUtils.colorToHexString(metadata.getInt(KEY_BACKGROUND_COLOR)),
                      ContrastUtils.CONTRAST_RATIO_WCAG_NORMAL_TEXT, /* Suggested for small text */
                      ContrastUtils.CONTRAST_RATIO_WCAG_LARGE_TEXT /* Suggested for large text */));
        }
        appendMetadataStringsToMessageIfNeeded(locale, metadata, builder);
        return builder.toString();
      case RESULT_ID_TEXTVIEW_HEURISTIC_CONTRAST_BORDERLINE:
        builder =
            new StringBuilder(
                String.format(
                    locale,
                    StringManager.getString(
                        locale, "result_message_textview_heuristic_contrast_not_sufficient"),
                    metadata.getDouble(KEY_CONTRAST_RATIO),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_FOREGROUND_COLOR)),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_BACKGROUND_COLOR)),
                    metadata.getDouble(KEY_REQUIRED_CONTRAST_RATIO), /* Suggested for small text */
                    metadata.getDouble(
                        KEY_TOLERANT_CONTRAST_RATIO) /* Suggested for large text */));
        appendMetadataStringsToMessageIfNeeded(locale, metadata, builder);
        return builder.toString();
      case RESULT_ID_CUSTOMIZED_TEXTVIEW_HEURISTIC_CONTRAST_NOT_SUFFICIENT:
        builder =
            new StringBuilder(
                String.format(
                    locale,
                    StringManager.getString(
                        locale,
                        "result_message_textview_heuristic_customized_contrast_not_sufficient"),
                    metadata.getDouble(KEY_CONTRAST_RATIO),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_FOREGROUND_COLOR)),
                    ContrastUtils.colorToHexString(metadata.getInt(KEY_BACKGROUND_COLOR)),
                    metadata.getDouble(KEY_CUSTOMIZED_HEURISTIC_CONTRAST_RATIO)));
        appendMetadataStringsToMessageIfNeeded(locale, metadata, builder);
        return builder.toString();
      default:
        throw new IllegalStateException("Unsupported result id");
    }
  }

  @Override
  public String getShortMessageForResultData(
      Locale locale, int resultId, @Nullable ResultMetadata metadata) {
    String generated = generateMessageForResultId(locale, resultId);
    if (generated != null) {
      return generated;
    }

    switch (resultId) {
      case RESULT_ID_TEXT_MUST_BE_OPAQUE:
        return StringManager.getString(locale, "result_message_text_must_be_opaque");
      case RESULT_ID_BACKGROUND_MUST_BE_OPAQUE:
        return StringManager.getString(locale, "result_message_background_must_be_opaque");
      case RESULT_ID_VIEW_NOT_WITHIN_SCREENCAPTURE:
        return StringManager.getString(locale, "result_message_no_screencapture"); // Close enough
      case RESULT_ID_TEXTVIEW_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_CUSTOMIZED_TEXTVIEW_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_TEXTVIEW_HEURISTIC_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_TEXTVIEW_HEURISTIC_CONTRAST_BORDERLINE:
      case RESULT_ID_CUSTOMIZED_TEXTVIEW_HEURISTIC_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_TEXTVIEW_UPPER_BOUND_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_CUSTOMIZED_TEXTVIEW_UPPER_BOUND_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_TEXTVIEW_LOWER_BOUND_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_CUSTOMIZED_TEXTVIEW_LOWER_BOUND_CONTRAST_NOT_SUFFICIENT:
        return StringManager.getString(locale, "result_message_brief_text_contrast_not_sufficient");
      default:
        throw new IllegalStateException("Unsupported result id");
    }
  }

  /**
   * Calculates a secondary priority for a text contrast result.
   *
   * <p>The result is the amount that the actual contrast ratio is below the required ratio. Thus,
   * for a given required contrast ratio, decreasing contrast gives a higher priority.
   */
  @Override
  public @Nullable Double getSecondaryPriority(AccessibilityHierarchyCheckResult result) {
    ResultMetadata metadata = result.getMetadata();
    switch (result.getResultId()) {
      case RESULT_ID_TEXTVIEW_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_TEXTVIEW_HEURISTIC_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_TEXTVIEW_HEURISTIC_CONTRAST_BORDERLINE:
      case RESULT_ID_TEXTVIEW_UPPER_BOUND_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_CUSTOMIZED_TEXTVIEW_UPPER_BOUND_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_TEXTVIEW_LOWER_BOUND_CONTRAST_NOT_SUFFICIENT:
      case RESULT_ID_CUSTOMIZED_TEXTVIEW_LOWER_BOUND_CONTRAST_NOT_SUFFICIENT:
        return checkNotNull(metadata).getDouble(KEY_REQUIRED_CONTRAST_RATIO, 0.0)
            - checkNotNull(metadata).getDouble(KEY_CONTRAST_RATIO, 0.0);
      case RESULT_ID_CUSTOMIZED_TEXTVIEW_HEURISTIC_CONTRAST_NOT_SUFFICIENT:
        return checkNotNull(metadata).getDouble(KEY_CUSTOMIZED_HEURISTIC_CONTRAST_RATIO, 0)
            - checkNotNull(metadata).getDouble(KEY_CONTRAST_RATIO, 0.0);
      default:
        return null;
    }
  }

  @Override
  public String getTitleMessage(Locale locale) {
    return StringManager.getString(locale, "check_title_text_contrast");
  }

  private static @Nullable String generateMessageForResultId(Locale locale, int resultId) {
    switch (resultId) {
      case RESULT_ID_NOT_VISIBLE:
        return StringManager.getString(locale, "result_message_not_visible");
      case RESULT_ID_NOT_TEXT_VIEW:
        return StringManager.getString(locale, "result_message_not_text_view");
      case RESULT_ID_NOT_ENABLED:
        return StringManager.getString(locale, "result_message_not_enabled");
      case RESULT_ID_TEXTVIEW_EMPTY:
        return StringManager.getString(locale, "result_message_textview_empty");
      case RESULT_ID_COULD_NOT_GET_TEXT_COLOR:
        return StringManager.getString(locale, "result_message_could_not_get_text_color");
      case RESULT_ID_COULD_NOT_GET_BACKGROUND_COLOR:
        return StringManager.getString(locale, "result_message_could_not_get_background_color");
      case RESULT_ID_HEURISTIC_COULD_NOT_GET_SCREENCAPTURE:
        return StringManager.getString(locale, "result_message_no_screencapture");
      case RESULT_ID_SCREENCAPTURE_DATA_HIDDEN:
        return StringManager.getString(locale, "result_message_screencapture_data_hidden");
      case RESULT_ID_SCREENCAPTURE_UNIFORM_COLOR:
        return StringManager.getString(locale, "result_message_screencapture_uniform_color");
      default:
        return null;
    }
  }

  /**
   * Checks if a view has any foreground color present in the text. It can be the color obtained
   * directly from the text or the color obtained from foreground color spans.
   *
   * @param view The {@link ViewHierarchyElement} to check
   * @param text The text span to check for foreground color spans
   */
  private static boolean hasAnyForegroundColor(ViewHierarchyElement view, SpannableString text) {
    if (getForegroundColor(view) != null) {
      return true;
    }
    if (text == null) {
      return false;
    }
    for (Span span : text.getSpans()) {
      if (span instanceof Spans.ForegroundColorSpan) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if a view has any background color present in the text. It can be the color obtained
   * directly from the text or the color obtained from background color spans.
   *
   * @param view The {@link ViewHierarchyElement} to check
   * @param text The text span to check for background color spans
   */
  private static boolean hasAnyBackgroundColor(ViewHierarchyElement view, SpannableString text) {
    if (view.getBackgroundDrawableColor() != null) {
      return true;
    }
    if (text == null) {
      return false;
    }
    for (Span span : text.getSpans()) {
      if (span instanceof Spans.BackgroundColorSpan) {
        return true;
      }
    }
    return false;
  }

  /**
   * Computes a list of foreground color ranges in a view. Each element is a foreground color along
   * with the range in which that color is present. The list is continuous in terms of the range. If
   * colored span detection is disabled, only the primary text color is present in the output list.
   *
   * @param text The text span to check for foreground color spans
   * @param textColor The primary text color obtained from a view
   * @return a list of {@link ColorRangeInfo} containing all foreground color ranges in a view
   */
  private static ImmutableList<ColorRangeInfo> getForegroundColorRangeInfos(
      SpannableString text, @Nullable Integer textColor) {
    return getColorRangeInfos(text, Spans.ForegroundColorSpan.class, textColor);
  }

  /**
   * Computes a list of background color ranges in a view. Each element is a background color along
   * with the range in which that color is present. The list is continuous in terms of the range. If
   * colored span detection is disabled, only the primary background color is present in the output
   * list.
   *
   * @param text The text span to check for background color spans
   * @param backgroundDrawableColor The primary background color obtained from a view
   * @return a list of {@link ColorRangeInfo} containing all background color ranges in a view
   */
  private static ImmutableList<ColorRangeInfo> getBackgroundColorRangeInfos(
      SpannableString text, @Nullable Integer backgroundDrawableColor) {
    return getColorRangeInfos(text, Spans.BackgroundColorSpan.class, backgroundDrawableColor);
  }

  private static ImmutableList<ColorRangeInfo> getColorRangeInfos(
      SpannableString text, Class<? extends Span> clazz, @Nullable Integer defaultColor) {
    ArrayList<ColorRangeInfo> colorRangeInfos =
        Lists.newArrayList(new ColorRangeInfo(0, text.length(), defaultColor));
    ArrayList<Span> filteredSpans =
        Lists.newArrayList(Collections2.filter(text.getSpans(), clazz::isInstance));
    if (filteredSpans.isEmpty()) {
      return ImmutableList.copyOf(colorRangeInfos);
    }

    for (Span span : filteredSpans) {
      int start = span.getStart();
      int end = span.getEnd();

      // Identify the range of ColorRangeInfos that are touched by the span.
      int startPtr = 0;
      int endPtr = colorRangeInfos.size() - 1;
      while (colorRangeInfos.get(startPtr).getEnd() < start) {
        startPtr++;
      }
      while (colorRangeInfos.get(endPtr).getStart() > end) {
        endPtr--;
      }
      ColorRangeInfo startRange = colorRangeInfos.get(startPtr);
      ColorRangeInfo endRange = colorRangeInfos.get(endPtr);

      // Remove the ColorRangeInfos from startPtr to endPtr
      for (int i = startPtr; i <= endPtr; i++) {
        colorRangeInfos.remove(startPtr);
      }

      // Replace those ColorRangeInfos with 1, 2 or 3 entries.
      int ptr = startPtr;
      if (startRange.getStart() < start) {
        colorRangeInfos.add(
            ptr++, new ColorRangeInfo(startRange.getStart(), start, startRange.getColor()));
      }
      colorRangeInfos.add(ptr++, new ColorRangeInfo(start, end, getSpanColor(span)));
      if (end < endRange.getEnd()) {
        colorRangeInfos.add(ptr, new ColorRangeInfo(end, endRange.getEnd(), endRange.getColor()));
      }
    }

    return ImmutableList.copyOf(colorRangeInfos);
  }

  /** Gets the color of a foreground or background color span. */
  private static int getSpanColor(Span span) {
    if (span instanceof Spans.ForegroundColorSpan) {
      return ((Spans.ForegroundColorSpan) span).getForegroundColor();
    }
    if (span instanceof Spans.BackgroundColorSpan) {
      return ((Spans.BackgroundColorSpan) span).getBackgroundColor();
    }
    throw new IllegalArgumentException("Unexpected Span type");
  }

  /**
   * Computes a list of all foreground and background color pair ranges based on overlap.
   *
   * @param foregroundColorRangeInfos The list of foreground color with corresponding range
   * @param backgroundColorRangeInfos The list of background color with corresponding range
   * @return a set of {@link ColorPair} containing all pairs of foreground and background color with
   *     overlapping ranges
   */
  private static ImmutableList<ColorPair> getColorPairs(
      ImmutableList<ColorRangeInfo> foregroundColorRangeInfos,
      ImmutableList<ColorRangeInfo> backgroundColorRangeInfos) {
    ImmutableList.Builder<ColorPair> colorPairs = new ImmutableList.Builder<>();
    int foregroundIndex = 0;
    int backgroundIndex = 0;
    while (foregroundIndex != foregroundColorRangeInfos.size()
        || backgroundIndex != backgroundColorRangeInfos.size()) {
      ColorRangeInfo foregroundInfo = foregroundColorRangeInfos.get(foregroundIndex);
      ColorRangeInfo backgroundInfo = backgroundColorRangeInfos.get(backgroundIndex);

      colorPairs.add(
          new ColorPair(
              max(foregroundInfo.getStart(), backgroundInfo.getStart()),
              min(foregroundInfo.getEnd(), backgroundInfo.getEnd()),
              foregroundInfo.getColor(),
              backgroundInfo.getColor()));
      if (foregroundInfo.getEnd() == backgroundInfo.getEnd()) {
        foregroundIndex += 1;
        backgroundIndex += 1;
      } else if (foregroundInfo.getEnd() < backgroundInfo.getEnd()) {
        foregroundIndex += 1;
      } else {
        backgroundIndex += 1;
      }
    }

    return colorPairs.build();
  }

  /**
   * Performs lightweight contrast evaluation on the provided {@code view}. During lightweight
   * evaluation, we examine text color and background Drawables to extract properties about
   * component colors.
   *
   * @param view The {@link ViewHierarchyElement} to evaluate
   * @param parameters Optional check input parameters
   * @return a list of {@link AccessibilityHierarchyCheckResult} describing the results of the
   *     lightweight evaluation, or empty list if there is sufficient text contrast.
   */
  private ImmutableList<AccessibilityHierarchyCheckResult> attemptLightweightEval(
      ViewHierarchyElement view, @Nullable Parameters parameters) {
    SpannableString text = TextUtils.isEmpty(view.getText()) ? view.getHintText() : view.getText();
    if (text == null) {
      return ImmutableList.of();
    }

    // Skip the check if no foreground colors are detected.
    if (!hasAnyForegroundColor(view, text)) {
      return ImmutableList.of(
          new AccessibilityHierarchyCheckResult(
              CHECK_CLASS,
              AccessibilityCheckResultType.NOT_RUN,
              view,
              RESULT_ID_COULD_NOT_GET_TEXT_COLOR,
              /* metadata= */ null));
    }

    // Skip the check if no background colors are detected.
    if (!hasAnyBackgroundColor(view, text)) {
      return ImmutableList.of(
          new AccessibilityHierarchyCheckResult(
              CHECK_CLASS,
              AccessibilityCheckResultType.NOT_RUN,
              view,
              RESULT_ID_COULD_NOT_GET_BACKGROUND_COLOR,
              /* metadata= */ null));
    }

    // Find all the foreground and background color pairs in the view text.
    ImmutableList<ColorPair> colorPairs =
        getColorPairs(
            getForegroundColorRangeInfos(text, getForegroundColor(view)),
            getBackgroundColorRangeInfos(text, view.getBackgroundDrawableColor()));

    ImmutableList.Builder<AccessibilityHierarchyCheckResult> results = ImmutableList.builder();
    for (ColorPair colorPair : colorPairs) {
      Integer foregroundColor = colorPair.getForegroundColor();
      Integer backgroundColor = colorPair.getBackgroundColor();
      if (foregroundColor == null || backgroundColor == null) {
        continue;
      }

      ResultMetadata resultMetadata = new HashMapResultMetadata();
      // Store the text being checked if the result corresponds to a proper substring of the view
      // text.
      if (colorPair.getStart() > 0 || colorPair.getEnd() < text.length()) {
        String substringText =
            text.subSequence(colorPair.getStart(), colorPair.getEnd()).toString();
        resultMetadata.putString(KEY_RESULT_TEXT_SUBSTRING, substringText);
      }

      // Skip the check if background is non-opaque. Contrast range evaluation will be triggered
      // while aggregating the results from lightweight eval.
      int backgroundAlpha = Color.alpha(backgroundColor);
      if (backgroundAlpha < OPAQUE_ALPHA) {
        resultMetadata.putFloat(KEY_BACKGROUND_OPACITY, (backgroundAlpha * 100f) / OPAQUE_ALPHA);
        resultMetadata.putInt(KEY_TEXT_COLOR, foregroundColor);
        resultMetadata.putInt(KEY_BACKGROUND_COLOR, backgroundColor);

        results.add(
            new AccessibilityHierarchyCheckResult(
                CHECK_CLASS,
                AccessibilityCheckResultType.NOT_RUN,
                view,
                RESULT_ID_BACKGROUND_MUST_BE_OPAQUE,
                resultMetadata));
        continue;
      }
      // Obtain the alpha blended text color and calculate the contrast ratio.
      int compositeTextColor = ContrastUtils.compositeColors(foregroundColor, backgroundColor);
      double contrastRatio =
          ContrastUtils.calculateContrastRatio(compositeTextColor, backgroundColor);
      double requiredContrast =
          Boolean.TRUE.equals(isLargeText(view))
              ? ContrastUtils.CONTRAST_RATIO_WCAG_LARGE_TEXT
              : ContrastUtils.CONTRAST_RATIO_WCAG_NORMAL_TEXT;
      // Set the required contrast ratio to a customized value if provided.
      Double customizedHeuristicContrastRatio =
          (parameters == null) ? null : parameters.getCustomTextContrastRatio();
      if (customizedHeuristicContrastRatio != null) {
        requiredContrast = customizedHeuristicContrastRatio;
      }
      // If calculated contrast ratio is less than required, add an error result to the results
      // list.
      if ((requiredContrast - contrastRatio) > CONTRAST_TOLERANCE) {
        resultMetadata.putDouble(
            (customizedHeuristicContrastRatio == null)
                ? KEY_REQUIRED_CONTRAST_RATIO
                : KEY_CUSTOMIZED_HEURISTIC_CONTRAST_RATIO,
            requiredContrast);
        resultMetadata.putDouble(KEY_CONTRAST_RATIO, contrastRatio);
        resultMetadata.putInt(KEY_TEXT_COLOR, foregroundColor);
        resultMetadata.putInt(KEY_BACKGROUND_COLOR, backgroundColor);

        results.add(
            new AccessibilityHierarchyCheckResult(
                CHECK_CLASS,
                AccessibilityCheckResultType.ERROR,
                view,
                (customizedHeuristicContrastRatio == null)
                    ? RESULT_ID_TEXTVIEW_CONTRAST_NOT_SUFFICIENT
                    : RESULT_ID_CUSTOMIZED_TEXTVIEW_CONTRAST_NOT_SUFFICIENT,
                resultMetadata));
      }
    }
    return results.build();
  }

  /**
   * Performs heavyweight contrast evaluation on the provided {@code view}. During heavyweight
   * evaluation, we examine the contents of a screen capture to heuristically determine foreground
   * and background colors, and from there, compute a contrast ratio.
   *
   * @param view The {@link ViewHierarchyElement} to evaluate
   * @param parameters Optional check input parameters
   * @return an {@link AccessibilityHierarchyCheckResult} describing the results of the heavyweight
   *     evaluation, or {@code null} if there is sufficient text contrast.
   */
  private @Nullable AccessibilityHierarchyCheckResult attemptHeavyweightEval(
      ViewHierarchyElement view, @Nullable Parameters parameters) {
    Image screenCapture = (parameters == null) ? null : parameters.getScreenCapture();
    if (screenCapture == null) {
      return new AccessibilityHierarchyCheckResult(
          CHECK_CLASS,
          AccessibilityCheckResultType.NOT_RUN,
          view,
          RESULT_ID_HEURISTIC_COULD_NOT_GET_SCREENCAPTURE,
          /* metadata= */ null);
    }
    Rect screenCaptureBounds = new Rect(0, 0, screenCapture.getWidth(), screenCapture.getHeight());
    Rect viewBounds = view.getBoundsInScreen();
    Rect textCharacterBounds = getTextCharacterBounds(view);
    if (!textCharacterBounds.isEmpty()
        && screenCaptureBounds.contains(textCharacterBounds)

        && Rect.intersects(viewBounds, textCharacterBounds)) {
      // Extracts foreground/background colors from the region which contains the text characters.
      viewBounds = textCharacterBounds;
    }

    if (viewBounds.isEmpty() || !screenCaptureBounds.contains(viewBounds)) {
      // If an off-screen view reports itself as visible, we shouldn't evaluate it.
      ResultMetadata resultMetadata = new HashMapResultMetadata();
      resultMetadata.putString(KEY_VIEW_BOUNDS_STRING, viewBounds.toShortString());
      resultMetadata.putString(KEY_SCREENSHOT_BOUNDS_STRING, screenCaptureBounds.toShortString());

      return new AccessibilityHierarchyCheckResult(
          CHECK_CLASS,
          AccessibilityCheckResultType.NOT_RUN,
          view,
          RESULT_ID_VIEW_NOT_WITHIN_SCREENCAPTURE,
          resultMetadata);
    }

    // viewBounds cannot be out of bounds because the bounds were checked above.
    Image viewImage = crop(screenCapture, viewBounds);
    ContrastSwatch contrastSwatch =
        getContrastSwatch(
            viewImage,
            (parameters == null) ? null : parameters.getEnableEnhancedContrastEvaluation());
    ResultMetadata resultMetadata = new HashMapResultMetadata();
    if (view.isAgainstScrollableEdge()) {
      resultMetadata.putBoolean(KEY_IS_AGAINST_SCROLLABLE_EDGE, true);
    }
    int foreground = contrastSwatch.getForegroundColors().get(0);
    int background = contrastSwatch.getBackgroundColor();

    if (background == foreground) {
      // In the case the foreground and background colors from ContrastSwatch are the same, and
      // they match the secure window censor color, we can assume the system has intentionally
      // censored our screen capture data, and we should not evaluate this content.
      return new AccessibilityHierarchyCheckResult(
          CHECK_CLASS,
          AccessibilityCheckResultType.NOT_RUN,
          view,
          (foreground == ContrastUtils.COLOR_SECURE_WINDOW_CENSOR)
              ? RESULT_ID_SCREENCAPTURE_DATA_HIDDEN
              : RESULT_ID_SCREENCAPTURE_UNIFORM_COLOR,
          resultMetadata);
    }

    ImmutableList<Integer> foregroundColors = contrastSwatch.getForegroundColors();
    ImmutableList<Double> contrastRatios = contrastSwatch.getContrastRatios();
    ArrayList<Integer> lowForegroundColors = new ArrayList<>();
    ArrayList<Double> lowContrastRatios = new ArrayList<>();

    Double customizedHeuristicContrastRatio =
        (parameters == null) ? null : parameters.getCustomTextContrastRatio();
    if (customizedHeuristicContrastRatio != null) {
      for (int i = 0; i < contrastRatios.size(); i++) {
        if (customizedHeuristicContrastRatio - contrastRatios.get(i) > CONTRAST_TOLERANCE) {
          lowForegroundColors.add(foregroundColors.get(i));
          lowContrastRatios.add(contrastRatios.get(i));
        }
      }
      if (!lowContrastRatios.isEmpty()) {
        if (isPotentiallyObscured(view)) {
          resultMetadata.putBoolean(KEY_IS_POTENTIALLY_OBSCURED, true);
        }
        resultMetadata.putDouble(
            KEY_CUSTOMIZED_HEURISTIC_CONTRAST_RATIO, customizedHeuristicContrastRatio);
        storeColorsAndContrastRatios(
            resultMetadata, background, lowForegroundColors, lowContrastRatios);
        return resultPossiblyWithImage(
            AccessibilityCheckResultType.WARNING,
            view,
            RESULT_ID_CUSTOMIZED_TEXTVIEW_HEURISTIC_CONTRAST_NOT_SUFFICIENT,
            resultMetadata,
            parameters,
            viewImage);
      }
    } else {
      double requiredContrastRatio = ContrastUtils.CONTRAST_RATIO_WCAG_LARGE_TEXT;
      boolean isTextSizeAvailable = (view.getTextSize() != null);
      if (isTextSizeAvailable) {
        resultMetadata.putBoolean(KEY_IS_LARGE_TEXT, checkNotNull(isLargeText(view)));
        requiredContrastRatio =
            checkNotNull(isLargeText(view))
                ? ContrastUtils.CONTRAST_RATIO_WCAG_LARGE_TEXT
                : ContrastUtils.CONTRAST_RATIO_WCAG_NORMAL_TEXT;
      }

      for (int i = 0; i < contrastRatios.size(); i++) {
        if (requiredContrastRatio - contrastRatios.get(i) > CONTRAST_TOLERANCE) {
          lowForegroundColors.add(foregroundColors.get(i));
          lowContrastRatios.add(contrastRatios.get(i));
        }
      }

      if (!lowContrastRatios.isEmpty()) {
        if (isPotentiallyObscured(view)) {
          resultMetadata.putBoolean(KEY_IS_POTENTIALLY_OBSCURED, true);
        }
        resultMetadata.putDouble(KEY_REQUIRED_CONTRAST_RATIO, requiredContrastRatio);
        storeColorsAndContrastRatios(
            resultMetadata, background, lowForegroundColors, lowContrastRatios);
        return resultPossiblyWithImage(
            AccessibilityCheckResultType.WARNING,
            view,
            RESULT_ID_TEXTVIEW_HEURISTIC_CONTRAST_NOT_SUFFICIENT,
            resultMetadata,
            parameters,
            viewImage);
      } else if (!isTextSizeAvailable) {
        for (int i = 0; i < contrastRatios.size(); i++) {
          if ((ContrastUtils.CONTRAST_RATIO_WCAG_NORMAL_TEXT - contrastRatios.get(i))
              > CONTRAST_TOLERANCE) {
            lowForegroundColors.add(foregroundColors.get(i));
            lowContrastRatios.add(contrastRatios.get(i));
          }
        }
        if (!lowContrastRatios.isEmpty()) {
          if (isPotentiallyObscured(view)) {
            resultMetadata.putBoolean(KEY_IS_POTENTIALLY_OBSCURED, true);
          }
          resultMetadata.putDouble(
              KEY_REQUIRED_CONTRAST_RATIO, ContrastUtils.CONTRAST_RATIO_WCAG_NORMAL_TEXT);
          resultMetadata.putDouble(
              KEY_TOLERANT_CONTRAST_RATIO, ContrastUtils.CONTRAST_RATIO_WCAG_LARGE_TEXT);
          storeColorsAndContrastRatios(
              resultMetadata, background, lowForegroundColors, lowContrastRatios);
          return resultPossiblyWithImage(
              AccessibilityCheckResultType.WARNING,
              view,
              RESULT_ID_TEXTVIEW_HEURISTIC_CONTRAST_BORDERLINE,
              resultMetadata,
              parameters,
              viewImage);
        }
      }
    }

    // Sufficient contrast
    return null;
  }

  /**
   * Performs contrast evaluation on the provided {@code view} by calculating the maximum & minimum
   * contrast ratios for a non-opaque background. We cannot obtain the exact contrast ratio for
   * non-opaque background as the backdrop is an unknown. The maximum and minimum values of contrast
   * ratio are calculated for the non opaque background and used as follows:
   *
   * <ul>
   *   <li>Case 1: Upper bound &lt; Required ratio: ERROR: Report this result
   *   <li>Case 2: Lower bound &lt; Required ratio: Run heavyweight eval and report heavyweight
   *       result if it is not null
   *   <li>Case 3: Both upper &amp; lower bound &gt;= Required ratio: Report no issue
   * </ul>
   *
   * @param view The {@link ViewHierarchyElement} to evaluate
   * @param parameters Optional check input parameters
   * @param lightweightResult An {@link AccessibilityHierarchyCheckResult} describing the
   *     lightweight result which triggered the contrast range eval
   * @return an {@link AccessibilityHierarchyCheckResult} describing the results of the contrast
   *     range evaluation, or {@code null} if there is sufficient text contrast.
   */
  private @Nullable AccessibilityHierarchyCheckResult attemptContrastRangeEval(
      ViewHierarchyElement view,
      @Nullable Parameters parameters,
      AccessibilityHierarchyCheckResult lightweightResult) {
    if (lightweightResult.getMetadata() == null) {
      return null;
    }
    int textColor = lightweightResult.getMetadata().getInt(KEY_TEXT_COLOR);
    int backgroundDrawableColor = lightweightResult.getMetadata().getInt(KEY_BACKGROUND_COLOR);
    String substringText = lightweightResult.getMetadata().getString(KEY_RESULT_TEXT_SUBSTRING, "");

    ResultMetadata resultMetadata = new HashMapResultMetadata();
    resultMetadata.putInt(KEY_TEXT_COLOR, textColor);
    resultMetadata.putInt(KEY_BACKGROUND_COLOR, backgroundDrawableColor);
    if (!substringText.isEmpty()) {
      resultMetadata.putString(KEY_RESULT_TEXT_SUBSTRING, substringText);
    }

    double requiredContrast =
        Boolean.TRUE.equals(isLargeText(view))
            ? ContrastUtils.CONTRAST_RATIO_WCAG_LARGE_TEXT
            : ContrastUtils.CONTRAST_RATIO_WCAG_NORMAL_TEXT;
    Double customizedHeuristicContrastRatio =
        (parameters == null) ? null : parameters.getCustomTextContrastRatio();
    if (customizedHeuristicContrastRatio != null) {
      requiredContrast = customizedHeuristicContrastRatio;
    }
    resultMetadata.putDouble(
        (customizedHeuristicContrastRatio == null)
            ? KEY_REQUIRED_CONTRAST_RATIO
            : KEY_CUSTOMIZED_HEURISTIC_CONTRAST_RATIO,
        requiredContrast);

    Range<Double> contrastRange =
        ContrastUtils.calculateContrastRatioRange(textColor, backgroundDrawableColor);
    double upperBound = contrastRange.upperEndpoint();
    double lowerBound = contrastRange.lowerEndpoint();
    if ((requiredContrast - upperBound) > CONTRAST_TOLERANCE) {
      resultMetadata.putDouble(KEY_CONTRAST_RATIO, upperBound);
      return new AccessibilityHierarchyCheckResult(
          CHECK_CLASS,
          AccessibilityCheckResultType.ERROR,
          view,
          (customizedHeuristicContrastRatio == null)
              ? RESULT_ID_TEXTVIEW_UPPER_BOUND_CONTRAST_NOT_SUFFICIENT
              : RESULT_ID_CUSTOMIZED_TEXTVIEW_UPPER_BOUND_CONTRAST_NOT_SUFFICIENT,
          resultMetadata);
    } else if ((requiredContrast - lowerBound) > CONTRAST_TOLERANCE) {
      resultMetadata.putDouble(KEY_CONTRAST_RATIO, lowerBound);
      return new AccessibilityHierarchyCheckResult(
          CHECK_CLASS,
          AccessibilityCheckResultType.WARNING,
          view,
          (customizedHeuristicContrastRatio == null)
              ? RESULT_ID_TEXTVIEW_LOWER_BOUND_CONTRAST_NOT_SUFFICIENT
              : RESULT_ID_CUSTOMIZED_TEXTVIEW_LOWER_BOUND_CONTRAST_NOT_SUFFICIENT,
          resultMetadata);
    } else {
      return null;
    }
  }

  @VisibleForTesting
  ContrastSwatch getContrastSwatch(
      Image image, @Nullable Boolean enableEnhancedContrastEvaluation) {
    return new ContrastSwatch(image, Boolean.TRUE.equals(enableEnhancedContrastEvaluation));
  }

  private Rect getTextCharacterBounds(ViewHierarchyElement view) {
    List<Rect> characterLocations = view.getTextCharacterLocations();
    if (characterLocations.isEmpty()) {
      return Rect.EMPTY;
    }

    int minLeft = Integer.MAX_VALUE;
    int minTop = Integer.MAX_VALUE;
    int maxRight = Integer.MIN_VALUE;
    int maxBottom = Integer.MIN_VALUE;
    for (Rect rect : characterLocations) {
      minLeft = min(minLeft, rect.getLeft());
      minTop = min(minTop, rect.getTop());
      maxRight = max(maxRight, rect.getRight());
      maxBottom = max(maxBottom, rect.getBottom());
    }
    return new Rect(minLeft, minTop, maxRight, maxBottom);
  }

  private Image crop(Image screenCapture, Rect viewBounds) {
    return screenCapture.crop(
        viewBounds.getLeft(), viewBounds.getTop(), viewBounds.getWidth(), viewBounds.getHeight());
  }

  /**
   * Returns a new instance of AccessibilityHierarchyCheckResultWithImage if {@code viewImage} is
   * not {@code null} and {@code parameters} specifies that view images should be saved. Otherwise,
   * returns a new instance of AccessibilityHierarchyCheckResult.
   */
  private AccessibilityHierarchyCheckResult resultPossiblyWithImage(
      AccessibilityCheckResultType type,
      ViewHierarchyElement view,
      int resultId,
      ResultMetadata metadata,
      @Nullable Parameters parameters,
      @Nullable Image viewImage) {
    if ((viewImage != null)
        && (parameters != null)
        && Boolean.TRUE.equals(parameters.getSaveViewImages())) {
      return new AccessibilityHierarchyCheckResultWithImage(
          CHECK_CLASS, type, view, resultId, metadata, viewImage);
    } else {
      return new AccessibilityHierarchyCheckResult(CHECK_CLASS, type, view, resultId, metadata);
    }
  }

  /** Stores extracted or estimated colors and contrast ratios in metadata. */
  private void storeColorsAndContrastRatios(
      ResultMetadata resultMetadata,
      int background,
      List<Integer> foregroundColors,
      List<Double> contrastRatios) {
    resultMetadata.putInt(KEY_BACKGROUND_COLOR, background);
    storeForegroundColors(resultMetadata, foregroundColors);
    storeContrastRatios(resultMetadata, contrastRatios);
  }

  /**
   * Stores foreground colors in metadata. The first color is stored with the key {@link
   * #KEY_FOREGROUND_COLOR}. If {@code foregroundColors} has more than one value, all but the first
   * value will be stored with the key {@link #KEY_ADDITIONAL_FOREGROUND_COLORS}.
   */
  private void storeForegroundColors(
      ResultMetadata resultMetadata, List<Integer> foregroundColors) {
    resultMetadata.putInt(KEY_FOREGROUND_COLOR, foregroundColors.get(0));
    if (foregroundColors.size() > 1) {
      resultMetadata.putStringList(
          KEY_ADDITIONAL_FOREGROUND_COLORS,
          Lists.transform(
              foregroundColors.subList(1, foregroundColors.size()), integer -> integer.toString()));
    }
  }

  /**
   * Stores contrast ratios in metadata. The first ratio is stored with the key {@link
   * #KEY_CONTRAST_RATIO}. If {@code contrastRatios} has more than one value, all but the first
   * value will be stored with the key {@link #KEY_ADDITIONAL_CONTRAST_RATIOS}.
   */
  private void storeContrastRatios(ResultMetadata resultMetadata, List<Double> contrastRatios) {
    resultMetadata.putDouble(KEY_CONTRAST_RATIO, contrastRatios.get(0));
    if (contrastRatios.size() > 1) {
      resultMetadata.putStringList(
          KEY_ADDITIONAL_CONTRAST_RATIOS,
          Lists.transform(
              contrastRatios.subList(1, contrastRatios.size()), ratio -> ratio.toString()));
    }
  }

  /**
   * Appends messages for {@link #KEY_IS_POTENTIALLY_OBSCURED} and {@link
   * #KEY_IS_AGAINST_SCROLLABLE_EDGE} to the provided {@code builder} if the relevant keys are set
   * in the given {@code resultMetadata}.
   *
   * @param builder the {@link StringBuilder} to which result messages should be appended
   */
  @SuppressWarnings("unused") // {@code locale} and {@code builder} may not be used
  private static void appendMetadataStringsToMessageIfNeeded(
      Locale locale, ResultMetadata resultMetadata, StringBuilder builder) {
    if (resultMetadata.getBoolean(KEY_IS_POTENTIALLY_OBSCURED, false)) {
      builder
          .append(' ')
          .append(
              StringManager.getString(locale, "result_message_addendum_view_potentially_obscured"));
    }
    if (resultMetadata.getBoolean(KEY_IS_AGAINST_SCROLLABLE_EDGE, false)) {
      builder
          .append(' ')
          .append(
              StringManager.getString(locale, "result_message_addendum_against_scrollable_edge"));
    }
  }

  private static @Nullable Boolean isLargeText(ViewHierarchyElement view) {
    Float textSize = view.getTextSize();
    if (textSize == null) {
      return null;
    }

    float density =
        view.getWindow()
            .getAccessibilityHierarchy()
            .getDeviceState()
            .getDefaultDisplayInfo()
            .getMetricsWithoutDecoration()
            .getScaledDensity();
    float dpSize = textSize / density;
    int style = (view.getTypefaceStyle() != null) ? view.getTypefaceStyle() : TYPEFACE_NORMAL;
    return (dpSize >= ContrastUtils.WCAG_LARGE_TEXT_MIN_SIZE)
        || ((dpSize >= ContrastUtils.WCAG_LARGE_BOLD_TEXT_MIN_SIZE)
            && ((style & TYPEFACE_BOLD) != 0));
  }

  /**
   * Returns the current color selected to paint the text or the hint of the given {@link
   * ViewHierarchyElement}.
   */
  private static @Nullable Integer getForegroundColor(ViewHierarchyElement view) {
    return TextUtils.isEmpty(view.getText()) ? view.getHintTextColor() : view.getTextColor();
  }

  /** A foreground-background color pair with the range it occurs in. */
  static final class ColorPair {
    private final int start;
    private final int end;
    private final @Nullable Integer fgColor;
    private final @Nullable Integer bgColor;

    ColorPair(int start, int end, @Nullable Integer fgColor, @Nullable Integer bgColor) {
      this.start = start;
      this.end = end;
      this.fgColor = fgColor;
      this.bgColor = bgColor;
    }

    int getStart() {
      return start;
    }

    int getEnd() {
      return end;
    }

    @Nullable Integer getForegroundColor() {
      return fgColor;
    }

    @Nullable Integer getBackgroundColor() {
      return bgColor;
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH, "ColorPair{range=[%d,%d] fg=%s bg=%s}", start, end, fgColor, bgColor);
    }
  }

  /** A text range and the color corresponding to the range. */
  static final class ColorRangeInfo {
    private final int start;
    private final int end;
    private final @Nullable Integer color;

    ColorRangeInfo(int start, int end, @Nullable Integer color) {
      this.start = start;
      this.end = end;
      this.color = color;
      checkState(start <= end, "start %s should be before end %s", start, end);
    }

    int getStart() {
      return start;
    }

    int getEnd() {
      return end;
    }

    @Nullable Integer getColor() {
      return color;
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "ColorRangeInfo{range=[%d,%d] color=%s}",
          start,
          end,
          color == null ? null : ContrastUtils.colorToHexString(color));
    }
  }
}
