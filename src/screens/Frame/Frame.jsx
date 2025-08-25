import React from "react";
import { Button } from "../../components/ui/button";
import { Card, CardContent } from "../../components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";

export const Frame = () => {
  const navigationItems = [
    { label: "종합정보", key: "overview" },
    { label: "관리", key: "management" },
    { label: "시간표", key: "schedule" },
    { label: "Q&A", key: "qna" },
    { label: "공지사항", key: "notice" },
  ];

  const userActions = [
    { label: "개인정보수정", key: "edit-profile" },
    { label: "회원탈퇴", key: "withdraw" },
  ];

  return (
    <div
      className="bg-white flex flex-row justify-center w-full"
      data-model-id="11:751"
    >
      <div className="bg-white w-[1920px] h-[1080px] relative">
        <header className="w-full h-[214px] flex flex-col">
          <div className="relative w-full h-[114px] flex items-center justify-center">
            <img
              className="w-[143px] h-[126px] object-cover"
              alt="Green Academy Logo"
              src="https://c.animaapp.com/nlUzuZMJ/img/image-13@2x.png"
            />
            <div className="ml-4 [font-family:'Inter',Helvetica] font-normal text-[#65e478] text-[32px] text-center tracking-[0] leading-[normal]">
              Green Academy Partner
            </div>
          </div>

          <nav className="w-full h-[100px] bg-[#65e478] flex items-center justify-center">
            <div className="flex space-x-[157px]">
              {navigationItems.map((item) => (
                <Button
                  key={item.key}
                  variant="ghost"
                  className="h-auto text-white text-4xl [font-family:'Inter',Helvetica] font-normal tracking-[0] leading-[normal] hover:bg-transparent hover:text-white/80"
                >
                  {item.label}
                </Button>
              ))}
            </div>
          </nav>
        </header>

        <aside className="absolute top-[241px] left-[17px]">
          <Card className="w-[281px] h-[322px] bg-[#f7f1f1] border-none">
            <CardContent className="p-0 h-full flex flex-col">
              <div className="p-4">
                <h2 className="text-black text-base text-center [font-family:'Inter',Helvetica] font-normal tracking-[0] leading-[normal]">
                  학원이름
                </h2>
              </div>

              <div className="flex-1 flex flex-col justify-end p-6 space-y-4">
                <div className="flex items-center justify-between">
                  {userActions.map((action, index) => (
                    <React.Fragment key={action.key}>
                      <Button
                        variant="ghost"
                        className="h-auto p-0 text-black text-[10px] [font-family:'Inter',Helvetica] font-normal tracking-[0] leading-[normal] hover:bg-transparent"
                      >
                        {action.label}
                      </Button>
                      {index === 0 && (
                        <img
                          className="w-[9px] h-2.5"
                          alt="Separator"
                          src="https://c.animaapp.com/nlUzuZMJ/img/polygon-7.svg"
                        />
                      )}
                    </React.Fragment>
                  ))}
                  <img
                    className="w-[9px] h-2.5"
                    alt="Separator"
                    src="https://c.animaapp.com/nlUzuZMJ/img/polygon-7.svg"
                  />
                </div>

                <Button className="w-full h-[51px] bg-[#65e478] hover:bg-[#5cd46b] text-white text-base [font-family:'Inter',Helvetica] font-normal tracking-[0] leading-[normal]">
                  로그아웃
                </Button>
              </div>
            </CardContent>
          </Card>
        </aside>

        <main className="absolute top-[214px] left-[536px] flex space-x-[10px]">
          <Select>
            <SelectTrigger className="w-[89px] h-7 bg-white border border-solid border-black">
              <SelectValue
                placeholder="학원"
                className="text-black text-xs [font-family:'Inter',Helvetica] font-normal tracking-[0] leading-[normal]"
              />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="academy1">학원 1</SelectItem>
              <SelectItem value="academy2">학원 2</SelectItem>
            </SelectContent>
          </Select>

          <Select>
            <SelectTrigger className="w-[89px] h-7 bg-white border border-solid border-black">
              <SelectValue
                placeholder="학생"
                className="text-black text-xs [font-family:'Inter',Helvetica] font-normal tracking-[0] leading-[normal]"
              />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="student1">학생 1</SelectItem>
              <SelectItem value="student2">학생 2</SelectItem>
            </SelectContent>
          </Select>
        </main>
      </div>
    </div>
  );
};
