package com.mishiranu.dashchan.chan.niuchan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class NiuchanBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<BoardCategory> mBoardCategories = new ArrayList<>();
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private String mBoardCategoryTitle;

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_CATEGORY = 1;
	
	private int mExpect = EXPECT_NONE;
	
	private static final Pattern BOARD_URI = Pattern.compile("/(\\w+)/");
	private static final Pattern NIUCHAN_BOARD_URI = Pattern.compile("http://niuchan.org/(\\w+)/");
	private static final Pattern NIUCHAN_BOARD_URI_HTTPS = Pattern.compile("https://niuchan.org/(\\w+)/");
	
	public NiuchanBoardsParser(String source)
	{
		mSource = source;
	}
	
	public ArrayList<BoardCategory> convert() throws ParseException
	{
		GroupParser.parse(mSource, this);
		closeCategory();
		for (BoardCategory boardCategory : mBoardCategories) Arrays.sort(boardCategory.getBoards());
		return mBoardCategories;
	}
	
	private void closeCategory()
	{
		if (mBoardCategoryTitle != null)
		{
			if (mBoards.size() > 0) mBoardCategories.add(new BoardCategory(mBoardCategoryTitle, mBoards));
			mBoardCategoryTitle = null;
			mBoards.clear();
		}
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("h2".equals(tagName))
		{
			closeCategory();
			mExpect = EXPECT_CATEGORY;
			return true;
		} else if ("a".equals(tagName)){
			if(attrs.toLowerCase().contains("href=\"https://niuchan.org/") || attrs.toLowerCase().contains("href=\"http://niuchan.org/")){//isBoardUri
				if (mBoardCategoryTitle != null)
				{
					String href = parser.getAttr(attrs, "href");
					int aHrefStartIndex = mSource.toLowerCase().indexOf("<a href=\"" + href.toLowerCase());
					int endATagIndex = mSource.toLowerCase().indexOf("</a>", aHrefStartIndex);
					String croppedSourceWithATag = mSource.substring(aHrefStartIndex, endATagIndex);
					String title = croppedSourceWithATag.substring(croppedSourceWithATag.indexOf("\">")+2).trim();

					if (title != null)
					{
						Matcher matcher = NIUCHAN_BOARD_URI.matcher(parser.getAttr(attrs, "href"));
						Matcher matcherHttps = NIUCHAN_BOARD_URI_HTTPS.matcher(parser.getAttr(attrs, "href"));
						if (matcher.matches())
						{
							String boardName = matcher.group(1);
//							if ("d".equals(boardName) || "sug".equals(boardName)) return false;
							title = StringUtils.clearHtml(title);
							mBoards.add(new Board(boardName, title));
						}
						else if (matcherHttps.matches())
						{
							String boardName = matcherHttps.group(1);
//							if ("d".equals(boardName) || "sug".equals(boardName)) return false;
							title = StringUtils.clearHtml(title);
							mBoards.add(new Board(boardName, title));
						}
					}
				}
			} else if (attrs.toLowerCase().contains("href=\"http://itachan.org")){
				closeCategory();
				mBoards.add(new Board("*", "Ruscello"));
				mBoardCategories.add(new BoardCategory("Altro", mBoards));
				mBoardCategoryTitle = null;
				mBoards.clear();
				mExpect = EXPECT_NONE;
				return true;

			}
		}

		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName)
	{
		
	}
	
	@Override
	public void onText(GroupParser parser, String source, int start, int end)
	{
		
	}
	
	@Override
	public void onGroupComplete(GroupParser parser, String text)
	{
		switch (mExpect)
		{
			case EXPECT_CATEGORY:
			{
				String noMinus = text;
				if(noMinus.contains("&minus;")){
					noMinus = noMinus.replace("&minus;", "");
				}
				String cleared = StringUtils.clearHtml(noMinus);
				String mText = cleared.trim();
				mBoardCategoryTitle = mText.trim();
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}