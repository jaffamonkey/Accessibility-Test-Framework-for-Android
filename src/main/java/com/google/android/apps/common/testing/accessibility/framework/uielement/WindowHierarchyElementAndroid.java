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

package com.google.android.apps.common.testing.accessibility.framework.uielement;

import static com.google.common.base.Preconditions.checkNotNull;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import com.google.android.apps.common.testing.accessibility.framework.replacements.Rect;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Representation of a {@link Window} hierarchy for accessibility checking
 *
 * <p>These windows hold references to surrounding {@link WindowHierarchyElementAndroid}s in its
 * local window hierarchy, the window's root {@link ViewHierarchyElementAndroid}, and the containing
 * {@link AccessibilityHierarchy}. An individual window may be uniquely identified in the context of
 * an {@link AccessibilityHierarchy} by the {@code id} value returned by {@link #getId()}.
 */
public class WindowHierarchyElementAndroid extends WindowHierarchyElement {

  private static final String TAG = "WindowHierarchyElementA";

  /* The id of each view corresponds to its position in this list */
  private final List<ViewHierarchyElementAndroid> viewHierarchyElements;

  // This field is set to a non-null value after construction.
  private @MonotonicNonNull AccessibilityHierarchyAndroid accessibilityHierarchy;

  private WindowHierarchyElementAndroid(
      int id,
      @Nullable Integer parentId,
      List<Integer> childIds,
      @Nullable Integer windowId,
      @Nullable Integer layer,
      @Nullable Integer type,
      @Nullable Boolean focused,
      @Nullable Boolean accessibilityFocused,
      @Nullable Boolean active,
      @Nullable Rect boundsInScreen,
      List<ViewHierarchyElementAndroid> viewHierarchyElements) {
    super(
        id,
        parentId,
        childIds,
        windowId,
        layer,
        type,
        focused,
        accessibilityFocused,
        active,
        boundsInScreen);
    this.viewHierarchyElements = viewHierarchyElements;
  }

  /**
   * @return The root {@link ViewHierarchyElementAndroid} of this window, or {@code null} if the
   *     window does not have a root view.
   * @see AccessibilityWindowInfo#getRoot()
   */
  @Override
  public @Nullable ViewHierarchyElementAndroid getRootView() {
    if (viewHierarchyElements.isEmpty()) {
      return null;
    }
    return viewHierarchyElements.get(0);
  }

  /**
   * Get all {@code ViewHierarchyElementAndroid}s in the window
   *
   * @return an unmodifiable {@link List} containing all {@link ViewHierarchyElementAndroid}s in
   *     this window, in depth-first ordering.
   */
  @Override
  public List<ViewHierarchyElementAndroid> getAllViews() {
    return Collections.unmodifiableList(viewHierarchyElements);
  }

  /**
   * @return The parent {@link WindowHierarchyElement} of this window, or {@code null} if this
   *     window is a root window.
   * @see AccessibilityWindowInfo#getParent()
   */
  @Override
  public @Nullable WindowHierarchyElementAndroid getParentWindow() {
    Integer parentIdTmp = parentId;
    return (parentIdTmp != null) ? getAccessibilityHierarchy().getWindowById(parentIdTmp) : null;
  }

  /**
   * @param atIndex The index of the child {@link WindowHierarchyElementAndroid} to obtain.
   * @return The requested child window
   * @throws NoSuchElementException if {@code atIndex} is less than 0 or greater than {@code
   *     getChildWindowCount() - 1}
   * @see AccessibilityWindowInfo#getChild(int)
   */
  @Override
  public WindowHierarchyElementAndroid getChildWindow(int atIndex) {
    if (atIndex < 0 || atIndex >= childIds.size()) {
      throw new NoSuchElementException();
    }
    return getAccessibilityHierarchy().getWindowById(childIds.get(atIndex));
  }

  /**
   * @param id The identifier for the desired {@link ViewHierarchyElementAndroid}, as returned by
   *     {@link ViewHierarchyElementAndroid#getId()}.
   * @return The {@link ViewHierarchyElementAndroid} identified by {@code id} in this window
   * @throws NoSuchElementException if no view within this window matches the provided {@code id}
   */
  @Override
  public ViewHierarchyElementAndroid getViewById(int id) {
    if ((id < 0) || (id >= viewHierarchyElements.size())) {
      throw new NoSuchElementException();
    }
    return viewHierarchyElements.get(id);
  }

  /** Returns the containing {@link AccessibilityHierarchyAndroid} of this window. */
  @Override
  public AccessibilityHierarchyAndroid getAccessibilityHierarchy() {

    // The type is explicit because the @MonotonicNonNull field is not read as @Nullable.
    return Preconditions.<@Nullable AccessibilityHierarchyAndroid>checkNotNull(
        accessibilityHierarchy);
  }

  /**
   * Retrieves the bounds of this window in absolute screen coordinates. Suitable for use in Android
   * runtime environments.
   *
   * @param outBounds The destination {@link android.graphics.Rect} into which the window's bounds
   *     are copied, or if this window has no bounds, {@code outBounds}' {@link
   *     android.graphics.Rect#isEmpty()} will return {@code true}.
   * @see
   *     android.view.accessibility.AccessibilityWindowInfo#getBoundsInScreen(android.graphics.Rect)
   */
  public void getBoundsInScreen(android.graphics.Rect outBounds) {
    if (boundsInScreen != null) {
      outBounds.set(
          new android.graphics.Rect(
              boundsInScreen.getLeft(),
              boundsInScreen.getTop(),
              boundsInScreen.getRight(),
              boundsInScreen.getBottom()));
    } else {
      outBounds.setEmpty();
    }
  }

  /** Set the containing {@link AccessibilityHierarchyAndroid} of this window. */
  void setAccessibilityHierarchy(AccessibilityHierarchyAndroid accessibilityHierarchy) {
    this.accessibilityHierarchy = accessibilityHierarchy;
  }

  /**
   * @param child The child {@link WindowHierarchyElementAndroid} to add as a child of this window
   */
  void addChild(WindowHierarchyElementAndroid child) {
    childIds.add(child.id);
  }

  /** Returns a new builder that can build a WindowHierarchyElementAndroid from a View. */
  static BuilderAndroid newBuilder(
      int id,
      View view,
      CustomViewBuilderAndroid customViewBuilder,
      AccessibilityNodeInfoExtraDataExtractor extraDataExtractor) {
    BuilderAndroid builder = new BuilderAndroid(id);
    builder.fromRootView = checkNotNull(view);
    builder.customViewBuilder = customViewBuilder;
    builder.aniExtraDataExtractor = extraDataExtractor;
    return builder;
  }

  /**
   * Returns a new builder that can build a WindowHierarchyElementAndroid from an
   * AccessibilityWindowInfo.
   */
  static BuilderAndroid newBuilder(
      int id,
      AccessibilityWindowInfo window,
      AccessibilityNodeInfoExtraDataExtractor extraDataExtractor) {
    BuilderAndroid builder = new BuilderAndroid(id);
    builder.fromWindowInfo = checkNotNull(window);
    builder.aniExtraDataExtractor = extraDataExtractor;
    return builder;
  }

  /**
   * Returns a new builder that can build a WindowHierarchyElementAndroid from an
   * AccessibilityNodeInfo.
   */
  static BuilderAndroid newBuilder(
      int id,
      AccessibilityNodeInfo nodeInfo,
      AccessibilityNodeInfoExtraDataExtractor extraDataExtractor) {
    BuilderAndroid builder = new BuilderAndroid(id);
    builder.fromNodeInfo = checkNotNull(nodeInfo);
    builder.aniExtraDataExtractor = extraDataExtractor;
    return builder;
  }

  /**
   * A builder for {@link WindowHierarchyElementAndroid}; obtained using {@link
   * WindowHierarchyElementAndroid#builder}.
   *
   * <p>This builder can construct an element from a View, AccessibilityNodeInfo or
   * AccessibilityWindowInfo. To build an element from a WindowHierarchyElementProto, use
   * WindowHierarchyElement.BuilderAndroid.
   */
  static class BuilderAndroid {
    private final int id;
    private @Nullable View fromRootView;
    private @Nullable CustomViewBuilderAndroid customViewBuilder;

    private @Nullable AccessibilityWindowInfo fromWindowInfo;
    private @Nullable AccessibilityNodeInfo fromNodeInfo;
    private @Nullable AccessibilityNodeInfoExtraDataExtractor aniExtraDataExtractor;
    private @Nullable WindowHierarchyElementAndroid parent;
    private @MonotonicNonNull Map<Long, AccessibilityNodeInfo> nodeInfoOriginMap;
    private @MonotonicNonNull Map<Long, View> viewOriginMap;
    private @MonotonicNonNull Map<AccessibilityNodeInfo, View> nodeToViewMap;

    private @Nullable Integer parentId;
    private final List<Integer> childIds = new ArrayList<>();
    private @Nullable Integer windowId;
    private @Nullable Integer layer;
    private @Nullable Integer type;
    private @Nullable Boolean focused;
    private @Nullable Boolean accessibilityFocused;
    private @Nullable Boolean active;
    private @Nullable Rect boundsInScreen;
    private List<ViewHierarchyElementAndroid> viewHierarchyElements;

    BuilderAndroid(int id) {
      super();
      this.id = id;
    }

    @CanIgnoreReturnValue
    public BuilderAndroid setParent(@Nullable WindowHierarchyElementAndroid parent) {
      this.parent = parent;
      return this;
    }

    @CanIgnoreReturnValue
    public BuilderAndroid setNodeInfoOriginMap(Map<Long, AccessibilityNodeInfo> originMap) {
      this.nodeInfoOriginMap = originMap;
      return this;
    }

    @CanIgnoreReturnValue
    public BuilderAndroid setViewOriginMap(Map<Long, View> originMap) {
      this.viewOriginMap = originMap;
      return this;
    }

    @CanIgnoreReturnValue
    public BuilderAndroid setNodeToViewMap(Map<AccessibilityNodeInfo, View> nodeToViewMap) {
      this.nodeToViewMap = nodeToViewMap;
      return this;
    }

    public WindowHierarchyElementAndroid build() {
      WindowHierarchyElementAndroid result;
      Map<ViewHierarchyElementAndroid, View> elementToViewMap = null;
      Map<ViewHierarchyElementAndroid, AccessibilityNodeInfo> elementToNodeInfoMap = null;

      if (fromRootView != null) {
        elementToViewMap = new HashMap<>();
        result =
            constructFromView(
                id,
                parent,
                fromRootView,
                elementToViewMap,
                checkNotNull(customViewBuilder),
                checkNotNull(aniExtraDataExtractor));
      } else if (fromWindowInfo != null) {
        elementToNodeInfoMap = new HashMap<>();
        result =
            constructFromWindow(
                id,
                parent,
                fromWindowInfo,
                elementToNodeInfoMap,
                checkNotNull(aniExtraDataExtractor));
      } else if (fromNodeInfo != null) {
        elementToNodeInfoMap = new HashMap<>();
        if (nodeToViewMap != null) {
          elementToViewMap = new HashMap<>();
        }
        result =
            constructFromNode(
                id,
                parent,
                fromNodeInfo,
                elementToNodeInfoMap,
                elementToViewMap,
                nodeToViewMap,
                checkNotNull(aniExtraDataExtractor));
      } else {
        throw new IllegalStateException("Nothing from which to build");
      }

      // Add entries to the origin maps after pointers to the window have been set.
      // The condensed unique IDs cannot be obtained without the window.
      setWindow(result);
      populateOriginMaps(elementToViewMap, elementToNodeInfoMap);
      return result;
    }

    /**
     * Creates a {@link ViewHierarchyElementAndroid.Builder} from a {@link View}.
     *
     * @param id The identifier for the desired {@link ViewHierarchyElementAndroid}
     * @param parent The {@link ViewHierarchyElementAndroid} corresponding to {@code forView}'s
     *     parent, or {@code null} if {@code forView} is a root view.
     * @param fromView The {@link View} from which to create the elements
     * @param customViewBuilder The {@link CustomViewBuilderAndroid} which customizes how to build
     *     an {@link AccessibilityHierarchyAndroid} from {@code forView}
     * @param extraDataExtractor The {@link AccessibilityNodeInfoExtraDataExtractor} for extracting
     *     extra rendering data
     * @return The newly created element
     */
    private static ViewHierarchyElementAndroid.Builder createViewHierarchyElementAndroidBuilder(
        int id,
        @Nullable ViewHierarchyElementAndroid parent,
        View fromView,
        CustomViewBuilderAndroid customViewBuilder,
        AccessibilityNodeInfoExtraDataExtractor extraDataExtractor) {
      return ViewHierarchyElementAndroid.newBuilder(
          id, parent, fromView, customViewBuilder, extraDataExtractor);
    }

    /**
     * Create a new {@link ViewHierarchyElementAndroid} from a {@link View} and appends it and its
     * VISIBLE children to {@code elementList}. The new elements' {@link
     * ViewHierarchyElementAndroid#getId()} will match their index in {@code elementList}. This also
     * adds the newly created elements as children to the provided {@code parent} element.
     *
     * @param forView The {@link View} from which to create the elements
     * @param elementList The list to hold the elements
     * @param parent The {@link ViewHierarchyElementAndroid} corresponding to {@code forView}'s
     *     parent, or {@code null} if {@code forView} is a root view.
     * @param elementToViewMap A {@link Map} to populate with the {@link
     *     ViewHierarchyElementAndroid}s created during construction of the hierarchy mapped to
     *     their originating {@link View}s
     * @param customViewBuilder The {@link CustomViewBuilderAndroid} which customizes how to build
     *     an {@link AccessibilityHierarchyAndroid} from {@code forView}
     * @param extraDataExtractor The {@link AccessibilityNodeInfoExtraDataExtractor} for extracting
     *     extra rendering data
     * @return The newly created element
     */
    @CanIgnoreReturnValue // Elements are added to elementList
    private static ViewHierarchyElementAndroid buildViewHierarchyFromView(
        View forView,
        List<ViewHierarchyElementAndroid> elementList,
        @Nullable ViewHierarchyElementAndroid parent,
        Map<ViewHierarchyElementAndroid, View> elementToViewMap,
        CustomViewBuilderAndroid customViewBuilder,
        AccessibilityNodeInfoExtraDataExtractor extraDataExtractor) {
      ViewHierarchyElementAndroid element =
          createViewHierarchyElementAndroidBuilder(
                  elementList.size(), parent, forView, customViewBuilder, extraDataExtractor)
              .build();
      elementList.add(element);
      elementToViewMap.put(element, forView);

      // Recurse for VISIBLE child views
      if (forView instanceof ViewGroup) {
        ViewGroup viewGroup = (ViewGroup) forView;
        for (int i = 0; i < viewGroup.getChildCount(); ++i) {
          View child = viewGroup.getChildAt(i);
          if (child.getVisibility() == View.VISIBLE) {
            element.addChild(
                buildViewHierarchyFromView(
                    child,
                    elementList,
                    element,
                    elementToViewMap,
                    customViewBuilder,
                    extraDataExtractor));
          }
        }
      }

      return element;
    }

    /**
     * Create a new {@link ViewHierarchyElementAndroid} from an {@link AccessibilityNodeInfo} and
     * appends it and its children to {@code elementList}. The new elements' {@link
     * ViewHierarchyElementAndroid#getId()} will match their index in {@code elementList}. This also
     * adds the newly created elements as children to the provided {@code parent} element.
     *
     * @param forInfo The non-null {@link AccessibilityNodeInfo} from which to create the elements
     * @param elementList The list to hold the elements
     * @param parent The {@link ViewHierarchyElementAndroid} corresponding to {@code forInfo}'s
     *     parent, or {@code null} if {@code forInfo} is a root view.
     * @param elementToNodeInfoMap A {@link Map} to populate with the {@link
     *     ViewHierarchyElementAndroid}s created during construction of the hierarchy mapped to
     *     their originating {@link AccessibilityNodeInfo}s
     * @param extraDataExtractor The {@link AccessibilityNodeInfoExtraDataExtractor} for extracting
     *     extra rendering data
     * @return The newly created element
     */
    @CanIgnoreReturnValue // Elements are added to elementList
    private static ViewHierarchyElementAndroid buildViewHierarchyFromNode(
        AccessibilityNodeInfo forInfo,
        List<ViewHierarchyElementAndroid> elementList,
        @Nullable ViewHierarchyElementAndroid parent,
        Map<ViewHierarchyElementAndroid, AccessibilityNodeInfo> elementToNodeInfoMap,
        @Nullable Map<ViewHierarchyElementAndroid, View> elementToViewMap,
        @Nullable Map<AccessibilityNodeInfo, View> nodeToViewMap,
        AccessibilityNodeInfoExtraDataExtractor extraDataExtractor) {
      View view = nodeToViewMap != null ? nodeToViewMap.get(forInfo) : null;
      ViewHierarchyElementAndroid element =
          ViewHierarchyElementAndroid.newBuilder(
                  elementList.size(), parent, forInfo, view, extraDataExtractor)
              .build();
      elementList.add(element);
      elementToNodeInfoMap.put(element, AccessibilityNodeInfo.obtain(forInfo));
      if ((elementToViewMap != null) && (view != null)) {
        elementToViewMap.put(element, view);
      }

      for (int i = 0; i < forInfo.getChildCount(); ++i) {
        AccessibilityNodeInfo child = forInfo.getChild(i);
        if (child != null) {
          element.addChild(
              buildViewHierarchyFromNode(
                  child,
                  elementList,
                  element,
                  elementToNodeInfoMap,
                  elementToViewMap,
                  nodeToViewMap,
                  extraDataExtractor));
        }
      }

      return element;
    }

    private WindowHierarchyElementAndroid constructFromWindow(
        int id,
        @Nullable WindowHierarchyElementAndroid parent,
        AccessibilityWindowInfo fromWindow,
        Map<ViewHierarchyElementAndroid, AccessibilityNodeInfo> elementToNodeInfoMap,
        AccessibilityNodeInfoExtraDataExtractor extraDataExtractor) {
      // Bookkeeping
      this.parentId = (parent != null) ? parent.getId() : null;

      // Window properties
      this.windowId = fromWindow.getId();
      this.layer = fromWindow.getLayer();
      this.type = fromWindow.getType();
      this.focused = fromWindow.isFocused();
      this.accessibilityFocused = fromWindow.isAccessibilityFocused();
      this.active = fromWindow.isActive();

      android.graphics.Rect tempRect = new android.graphics.Rect();
      fromWindow.getBoundsInScreen(tempRect);
      this.boundsInScreen = new Rect(tempRect.left, tempRect.top, tempRect.right, tempRect.bottom);

      // Build the window's view hierarchy
      AccessibilityNodeInfo rootInfo = fromWindow.getRoot();
      this.viewHierarchyElements = new ArrayList<>(); // The ultimate size is unknown
      if (rootInfo != null) {
        buildViewHierarchyFromNode(
            rootInfo,
            viewHierarchyElements,
            /* parent= */ null,
            elementToNodeInfoMap,
            /* elementToViewMap= */ null,
            /* nodeToViewMap= */ null,
            extraDataExtractor);
      } else {
        // This could occur in the case where the application state changes between the time that
        // the AccessibilityWindowInfo object is obtained and when its root AccessibilityNodeInfo is
        // extracted.
        LogUtils.w(TAG, "Constructed WindowHierarchyElement with no valid root.");
      }
      return new WindowHierarchyElementAndroid(
          id,
          parentId,
          childIds,
          windowId,
          layer,
          type,
          focused,
          accessibilityFocused,
          active,
          boundsInScreen,
          viewHierarchyElements);
    }

    private WindowHierarchyElementAndroid constructFromNode(
        int id,
        @Nullable WindowHierarchyElementAndroid parent,
        AccessibilityNodeInfo fromRootNode,
        Map<ViewHierarchyElementAndroid, AccessibilityNodeInfo> elementToNodeInfoMap,
        @Nullable Map<ViewHierarchyElementAndroid, View> elementToViewMap,
        @Nullable Map<AccessibilityNodeInfo, View> nodeToViewMap,
        AccessibilityNodeInfoExtraDataExtractor extraDataExtractor) {
      // Bookkeeping
      this.parentId = (parent != null) ? parent.getId() : null;

      // Window properties
      this.windowId = fromRootNode.getWindowId();
      android.graphics.Rect tempRect = new android.graphics.Rect();
      fromRootNode.getBoundsInScreen(tempRect);
      this.boundsInScreen = new Rect(tempRect.left, tempRect.top, tempRect.right, tempRect.bottom);

      // We make the assumption that if we're passed a root node, it's coming from the active window
      // of an application.
      this.active = true;
      this.type = WindowHierarchyElement.WINDOW_TYPE_APPLICATION;

      // We can't evaluate other window properties from an AccessibilityNodeInfo instance.
      this.layer = null;
      this.focused = null;
      this.accessibilityFocused = null;

      // Build the window's view hierarchy
      this.viewHierarchyElements = new ArrayList<>(); // The ultimate size is unknown
      buildViewHierarchyFromNode(
          fromRootNode,
          viewHierarchyElements,
          /* parent= */ null,
          elementToNodeInfoMap,
          elementToViewMap,
          nodeToViewMap,
          extraDataExtractor);
      return new WindowHierarchyElementAndroid(
          id,
          parentId,
          childIds,
          windowId,
          layer,
          type,
          focused,
          accessibilityFocused,
          active,
          boundsInScreen,
          viewHierarchyElements);
    }

    private WindowHierarchyElementAndroid constructFromView(
        int id,
        @Nullable WindowHierarchyElementAndroid parent,
        View fromRootView,
        Map<ViewHierarchyElementAndroid, View> elementToViewMap,
        CustomViewBuilderAndroid customViewBuilder,
        AccessibilityNodeInfoExtraDataExtractor extraDataExtractor) {
      // Bookkeeping
      this.parentId = (parent != null) ? parent.getId() : null;

      // Window properties
      android.graphics.Rect tempRect = new android.graphics.Rect();
      fromRootView.getWindowVisibleDisplayFrame(tempRect);
      this.boundsInScreen = new Rect(tempRect.left, tempRect.top, tempRect.right, tempRect.bottom);

      // We make the assumption that if we have an instance of View, it's coming from the active
      // window of an application.
      this.active = true;
      this.type = WindowHierarchyElement.WINDOW_TYPE_APPLICATION;

      // We can't evaluate other window properties from a View instance.
      this.windowId = null;
      this.layer = null;
      this.focused = null;
      this.accessibilityFocused = null;
      this.viewHierarchyElements = new ArrayList<>(); // The ultimate size is unknown

      buildViewHierarchyFromView(
          fromRootView,
          viewHierarchyElements,
          /* parent= */ null,
          elementToViewMap,
          customViewBuilder,
          extraDataExtractor);
      return new WindowHierarchyElementAndroid(
          id,
          parentId,
          childIds,
          windowId,
          layer,
          type,
          focused,
          accessibilityFocused,
          active,
          boundsInScreen,
          viewHierarchyElements);
    }

    private void populateOriginMaps(
        @Nullable Map<ViewHierarchyElementAndroid, View> elementToViewMap,
        @Nullable Map<ViewHierarchyElementAndroid, AccessibilityNodeInfo> elementToNodeInfoMap) {
      if (viewOriginMap != null) {
        for (Map.Entry<ViewHierarchyElementAndroid, View> entry :
            checkNotNull(elementToViewMap).entrySet()) {
          viewOriginMap.put(entry.getKey().getCondensedUniqueId(), entry.getValue());
        }
      }
      if (nodeInfoOriginMap != null) {
        for (Map.Entry<ViewHierarchyElementAndroid, AccessibilityNodeInfo> entry :
            checkNotNull(elementToNodeInfoMap).entrySet()) {
          nodeInfoOriginMap.put(entry.getKey().getCondensedUniqueId(), entry.getValue());
        }
      }
    }

    /** Set backpointers from the window's views to the window. */
    private static void setWindow(WindowHierarchyElementAndroid window) {
      if (window.viewHierarchyElements != null) {
        for (ViewHierarchyElementAndroid view : window.viewHierarchyElements) {
          view.setWindow(window);
        }
      }
    }
  }
}
