/*
 * Copyright (C) 2007-2011 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.fbreader;

import java.util.*;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.util.ZLHyperlinkHistoryManager;
import org.geometerplus.zlibrary.core.filesystem.*;
import org.geometerplus.zlibrary.core.application.*;
import org.geometerplus.zlibrary.core.dialogs.ZLDialogManager;
import org.geometerplus.zlibrary.core.options.*;

import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.library.Library;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.Bookmark;

public final class FBReaderApp extends ZLApplication {
	public final ZLBooleanOption AllowScreenBrightnessAdjustmentOption =
		new ZLBooleanOption("LookNFeel", "AllowScreenBrightnessAdjustment", true);
	public final ZLStringOption TextSearchPatternOption =
		new ZLStringOption("TextSearch", "Pattern", "");

	public final ZLBooleanOption UseSeparateBindingsOption =
		new ZLBooleanOption("KeysOptions", "UseSeparateBindings", false);

	public final ZLBooleanOption EnableDoubleTapOption =
		new ZLBooleanOption("Options", "EnableDoubleTap", false);
	public final ZLBooleanOption NavigateAllWordsOption =
		new ZLBooleanOption("Options", "NavigateAllWords", false);

	public enum DictionaryTappingAction {
		doNothing, selectWord, openDictionary
	}
	public final ZLEnumOption<DictionaryTappingAction> DictionaryTappingActionOption =
		new ZLEnumOption<DictionaryTappingAction>("Options", "DictionaryTappingAction", DictionaryTappingAction.selectWord);

	public final ZLIntegerRangeOption LeftMarginOption =
		new ZLIntegerRangeOption("Options", "LeftMargin", 0, 30, 4);
	public final ZLIntegerRangeOption RightMarginOption =
		new ZLIntegerRangeOption("Options", "RightMargin", 0, 30, 4);
	public final ZLIntegerRangeOption TopMarginOption =
		new ZLIntegerRangeOption("Options", "TopMargin", 0, 30, 0);
	public final ZLIntegerRangeOption BottomMarginOption =
		new ZLIntegerRangeOption("Options", "BottomMargin", 0, 30, 4);

	public final ZLIntegerRangeOption ScrollbarTypeOption =
		new ZLIntegerRangeOption("Options", "ScrollbarType", 0, 3, FBView.SCROLLBAR_SHOW_AS_FOOTER);
	public final ZLIntegerRangeOption FooterHeightOption =
		new ZLIntegerRangeOption("Options", "FooterHeight", 8, 20, 9);
	public final ZLBooleanOption FooterShowTOCMarksOption =
		new ZLBooleanOption("Options", "FooterShowTOCMarks", true);
	public final ZLIntegerRangeOption FooterLongTapOption =
		new ZLIntegerRangeOption("Options", "FooterLongTap", 0, 1, 0/*revert*/);
	public final ZLBooleanOption FooterShowClockOption =
		new ZLBooleanOption("Options", "ShowClockInFooter", true);
	public final ZLBooleanOption FooterShowBatteryOption =
		new ZLBooleanOption("Options", "ShowBatteryInFooter", true);
	public final ZLBooleanOption FooterShowProgressOption =
		new ZLBooleanOption("Options", "ShowProgressInFooter", true);
	public final ZLBooleanOption FooterIsSensitiveOption =
		new ZLBooleanOption("Options", "FooterIsSensitive", false);
	public final ZLStringOption FooterFontOption =
		new ZLStringOption("Options", "FooterFont", "Droid Sans");

	final ZLBooleanOption SelectionEnabledOption =
		new ZLBooleanOption("Options", "IsSelectionEnabled", true);

	final ZLStringOption ColorProfileOption =
		new ZLStringOption("Options", "ColorProfile", ColorProfile.DAY);

	public final ZLBooleanOption ShowPreviousBookInCancelMenuOption =
		new ZLBooleanOption("CancelMenu", "previousBook", false);
	public final ZLBooleanOption ShowPositionsInCancelMenuOption =
		new ZLBooleanOption("CancelMenu", "positions", true);

	private final ZLKeyBindings myBindings = new ZLKeyBindings("Keys");

	public final FBView BookTextView;
	public final FBView FootnoteView;
	public final ZLHyperlinkHistoryManager HyperlinkHistory;

	public BookModel Model;

	private final String myArg0;

	public FBReaderApp(String arg) {
		myArg0 = arg;

		addAction(ActionCode.INCREASE_FONT, new ChangeFontSizeAction(this, +2));
		addAction(ActionCode.DECREASE_FONT, new ChangeFontSizeAction(this, -2));
		addAction(ActionCode.ROTATE, new RotateAction(this));

		addAction(ActionCode.FIND_NEXT, new FindNextAction(this));
		addAction(ActionCode.FIND_PREVIOUS, new FindPreviousAction(this));
		addAction(ActionCode.CLEAR_FIND_RESULTS, new ClearFindResultsAction(this));

		addAction(ActionCode.TURN_PAGE_FORWARD, new TurnPageAction(this, true));
		addAction(ActionCode.TURN_PAGE_BACK, new TurnPageAction(this, false));

		addAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD, new VolumeKeyTurnPageAction(this, true));
		addAction(ActionCode.VOLUME_KEY_SCROLL_BACK, new VolumeKeyTurnPageAction(this, false));

		//addAction(ActionCode.COPY_SELECTED_TEXT_TO_CLIPBOARD, new DummyAction(this));
		//addAction(ActionCode.OPEN_SELECTED_TEXT_IN_DICTIONARY, new DummyAction(this));
		//addAction(ActionCode.CLEAR_SELECTION, new DummyAction(this));

		addAction(ActionCode.SWITCH_TO_DAY_PROFILE, new SwitchProfileAction(this, ColorProfile.DAY));
		addAction(ActionCode.SWITCH_TO_NIGHT_PROFILE, new SwitchProfileAction(this, ColorProfile.NIGHT));

		BookTextView = new FBView(this);
		FootnoteView = new FBView(this);
		HyperlinkHistory = ZLHyperlinkHistoryManager.Instance();

		setView(BookTextView);
	}

	public void initWindow() {
		super.initWindow();
		ZLDialogManager.Instance().wait("loadingBook", new Runnable() {
			public void run() {
				Book book = createBookForFile(ZLFile.createFileByPath(myArg0));
				if (book == null) {
					book = Library.getRecentBook();
				}
				if ((book == null) || !book.File.exists()) {
					book = Book.getByFile(Library.getHelpFile());
				}
				openBookInternal(book, null);
			}
		});
	}

	public void openBook(final Book book, final Bookmark bookmark) {
		if (book == null) {
			return;
		}
		if (Model != null) {
			if (bookmark == null & book.File.getPath().equals(Model.Book.File.getPath())) {
				return;
			}
		}
		ZLDialogManager.Instance().wait("loadingBook", new Runnable() {
			public void run() {
				openBookInternal(book, bookmark);
			}
		});
	}

	private ColorProfile myColorProfile;

	public ColorProfile getColorProfile() {
		if (myColorProfile == null) {
			myColorProfile = ColorProfile.get(getColorProfileName());
		}
		return myColorProfile;
	}

	public String getColorProfileName() {
		return ColorProfileOption.getValue();
	}

	public void setColorProfileName(String name) {
		ColorProfileOption.setValue(name);
		myColorProfile = null;
	}

	public ZLKeyBindings keyBindings() {
		return myBindings;
	}

	public FBView getTextView() {
		return (FBView)getCurrentView();
	}

	public void tryOpenFootnote(String id) {
		if (Model != null) {
			BookModel.Label label = Model.getLabel(id);
			if (label != null) {
				addInvisibleBookmark();
				if (label.ModelId == null) {
					BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
				} else {
					FootnoteView.setModel(Model.getFootnoteModel(label.ModelId));
					setView(FootnoteView);
					FootnoteView.gotoPosition(label.ParagraphIndex, 0, 0);
				}
				repaintView();
			}
		}
	}

	public void clearTextCaches() {
		BookTextView.clearCaches();
		FootnoteView.clearCaches();
	}

	void openBookInternal(Book book, Bookmark bookmark) {
		if (book != null) {
			onViewChanged();

			if (Model != null) {
				Model.Book.storePosition(BookTextView.getStartCursor());
				Model.Book.storeVisitedLinks();
				Model.Book.storeLinkHistory();
			}
			BookTextView.setModel(null);
			FootnoteView.setModel(null);
			clearTextCaches();

			Model = null;
			System.gc();
			System.gc();
			Model = BookModel.createModel(book);
			if (Model != null) {
				ZLTextHyphenator.Instance().load(book.getLanguage());
				book.loadLinkHistory();
				book.loadVisitedLinks();
				BookTextView.setModel(Model.BookTextModel);
				BookTextView.gotoPosition(book.getStoredPosition());
				if (bookmark == null) {
					setView(BookTextView);
				} else {
					gotoBookmark(bookmark);
				}
				Library.addBookToRecentList(book);
			}
		}
		repaintView();
	}

	public void gotoBookmark(Bookmark bookmark) {
		addInvisibleBookmark();
		final String modelId = bookmark.ModelId;
		if (modelId == null) {
			BookTextView.gotoPosition(bookmark);
			setView(BookTextView);
		} else {
			FootnoteView.setModel(Model.getFootnoteModel(modelId));
			FootnoteView.gotoPosition(bookmark);
			setView(FootnoteView);
		}
		repaintView();
	}

	public void showBookTextView() {
		setView(BookTextView);
	}

	public void gotoPosition(ZLTextPosition position) {
		BookTextView.gotoPosition(position);
		setView(BookTextView);
	}

	public ZLTextPosition getPosition() {
		return BookTextView.getStartCursor();
	}

	private Book createBookForFile(ZLFile file) {
		if (file == null) {
			return null;
		}
		Book book = Book.getByFile(file);
		if (book != null) {
			book.insertIntoBookList();
			return book;
		}
		if (file.isArchive()) {
			for (ZLFile child : file.children()) {
				book = Book.getByFile(child);
				if (book != null) {
					book.insertIntoBookList();
					return book;
				}
			}
		}
		return null;
	}

	@Override
	public void openFile(ZLFile file) {
		final Book book = createBookForFile(file);
		if (book != null) {
			openBook(book, null);
		}
	}

	public void onWindowClosing() {
		if (Model != null && BookTextView != null) {
			Model.Book.storePosition(BookTextView.getStartCursor());
			Model.Book.storeVisitedLinks();
			Model.Book.storeLinkHistory();
		}
	}

	static enum CancelActionType {
		previousBook,
		returnTo,
		back,
		forward,
		close
	}

	public static class CancelActionDescription {
		final CancelActionType Type;
		public final String Title;
		public final String Summary;
		public final boolean Enabled;

		CancelActionDescription(CancelActionType type, String summary, boolean enabled) {
			final ZLResource resource = ZLResource.resource("cancelMenu");
			Type = type;
			Title = resource.getResource(type.toString()).getValue();
			Summary = summary;
			Enabled = enabled;
		}
	}

	private static class BookmarkDescription extends CancelActionDescription {
		final Bookmark Bookmark;
		
		BookmarkDescription(Bookmark b) {
			super(CancelActionType.returnTo, b.getText(), true);
			Bookmark = b;
		}
	}

	private final ArrayList<CancelActionDescription> myCancelActionsList =
		new ArrayList<CancelActionDescription>();

	public List<CancelActionDescription> getCancelActionsList() {
		myCancelActionsList.clear();
		if (ShowPreviousBookInCancelMenuOption.getValue()) {
			final Book previousBook = Library.getPreviousBook();
			if (previousBook != null) {
				myCancelActionsList.add(new CancelActionDescription(
					CancelActionType.previousBook, previousBook.getTitle()
				));
			}
		}
		if (ShowPositionsInCancelMenuOption.getValue()) {
			if (Model != null && Model.Book != null) {
				for (Bookmark bookmark : Bookmark.invisibleBookmarks(Model.Book)) {
					myCancelActionsList.add(new BookmarkDescription(bookmark));
				}
			}
		}
		myCancelActionsList.add(new CancelActionDescription(
			CancelActionType.back, null, HyperlinkHistory.hasBackHistory()
		));
		myCancelActionsList.add(new CancelActionDescription(
			CancelActionType.forward, null, HyperlinkHistory.hasForwardHistory()
		));
		myCancelActionsList.add(new CancelActionDescription(
			CancelActionType.close, null, true
		));
		return myCancelActionsList;
	}

	public void runCancelAction(int index) {
		if (index < 0 || index >= myCancelActionsList.size()) {
			return;
		}

		final CancelActionDescription description = myCancelActionsList.get(index);
		ZLTextPosition position;
		switch (description.Type) {
			case previousBook:
				openBook(Library.getPreviousBook(), null);
				break;
			case back:
				position = getTextView().getStartCursor();
				position = HyperlinkHistory.back(position);
				BookTextView.gotoPosition(position);
				setView(BookTextView);
				break;
			case forward:
				position = getTextView().getStartCursor();
				position = HyperlinkHistory.forward(position);
				BookTextView.gotoPosition(position);
				setView(BookTextView);
				break;
			case returnTo:
			{
				final Bookmark b = ((BookmarkDescription)description).Bookmark;
				b.delete();
				gotoBookmark(b);
				break;
			}
			case close:
				closeWindow();
				break;
		}
	}

	private void updateInvisibleBookmarksList(Bookmark b) {
		if (Model.Book != null && b != null) {
			for (Bookmark bm : Bookmark.invisibleBookmarks(Model.Book)) {
				if (b.equals(bm)) {
					bm.delete();
				}
			}
			b.save();
			final List<Bookmark> bookmarks = Bookmark.invisibleBookmarks(Model.Book);
			for (int i = 3; i < bookmarks.size(); ++i) {
				bookmarks.get(i).delete();
			}
		}
	}

	public void addInvisibleBookmark(ZLTextWordCursor cursor) {
		if (cursor != null && Model != null && Model.Book != null && getTextView() == BookTextView) {
			updateInvisibleBookmarksList(new Bookmark(
				Model.Book,
				getTextView().getModel().getId(),
				cursor,
				6,
				false
			));
		}
	}

	public void addInvisibleBookmark() {
		if (Model.Book != null && getTextView() == BookTextView) {
			updateInvisibleBookmarksList(addBookmark(6, false));
		}
	}

	public Bookmark addBookmark(int maxLength, boolean visible) {
		final FBView view = getTextView();
		final ZLTextWordCursor cursor = view.getStartCursor();

		if (cursor.isNull()) {
			return null;
		}

		return new Bookmark(
			Model.Book,
			view.getModel().getId(),
			cursor,
			maxLength,
			visible
		);
	}
}
